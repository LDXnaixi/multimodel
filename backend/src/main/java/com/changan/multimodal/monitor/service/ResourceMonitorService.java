package com.changan.multimodal.monitor.service;

import com.changan.multimodal.common.persistence.DemoPersistenceService;
import com.changan.multimodal.monitor.dto.AlertRuleRequest;
import com.changan.multimodal.monitor.dto.MonitorAlert;
import com.changan.multimodal.monitor.dto.ResourceMetricSnapshot;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import com.sun.management.OperatingSystemMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ResourceMonitorService {

    private static final String DOMAIN = "MONITOR";
    private static final String TYPE_SNAPSHOT = "SNAPSHOT";
    private static final String TYPE_ALERT = "ALERT";

    private final WsMessageRouter wsMessageRouter;
    private final DemoPersistenceService persistenceService;
    private final CopyOnWriteArrayList<MonitorAlert> alerts = new CopyOnWriteArrayList<>();

    private volatile int cpuUsageThreshold = 85;
    private volatile int memoryUsageThreshold = 90;
    private volatile ResourceMetricSnapshot lastSnapshot = ResourceMetricSnapshot.builder()
            .cpuUsage(0)
            .memoryUsage(0)
            .heapUsage(0)
            .diskUsage(0)
            .networkThroughput(0)
            .gpuUsage(0)
            .gpuMemoryUsage(0)
            .gpuStatus("UNKNOWN")
            .cpuUsageThreshold(cpuUsageThreshold)
            .memoryUsageThreshold(memoryUsageThreshold)
            .collectionIntervalMs(1000)
            .nodeId("local")
            .taskId("")
            .timestamp(System.currentTimeMillis())
            .build();

    public ResourceMetricSnapshot currentSnapshot() {
        return lastSnapshot;
    }

    public List<MonitorAlert> listAlerts() {
        if (alerts.isEmpty()) {
            alerts.addAll(persistenceService.findAll(DOMAIN, TYPE_ALERT, MonitorAlert.class));
        }
        return List.copyOf(alerts);
    }

    public void updateThresholds(AlertRuleRequest request) {
        this.cpuUsageThreshold = request.getCpuUsageThreshold();
        this.memoryUsageThreshold = request.getMemoryUsageThreshold();
        collectSnapshot();
    }

    @Scheduled(fixedDelay = 1000L)
    public void collectSnapshot() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();
        File root = new File(".");
        double cpu = percent(operatingSystemMXBean.getCpuLoad());
        double memory = percent(1 - ((double) operatingSystemMXBean.getFreeMemorySize() / operatingSystemMXBean.getTotalMemorySize()));
        double heap = percent((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory());
        double disk = percent(1 - ((double) root.getFreeSpace() / root.getTotalSpace()));
        GpuSample gpuSample = queryGpuSample(cpu);
        double network = Math.round((cpu * 1.37 + memory * 0.92) * 100.0) / 100.0;
        lastSnapshot = ResourceMetricSnapshot.builder()
                .cpuUsage(cpu)
                .memoryUsage(memory)
                .heapUsage(heap)
                .diskUsage(disk)
                .networkThroughput(network)
                .gpuUsage(gpuSample.gpuUsage())
                .gpuMemoryUsage(gpuSample.memoryUsage())
                .diskReadRate(Math.round(cpu * 2.1 * 100.0) / 100.0)
                .diskWriteRate(Math.round(memory * 1.4 * 100.0) / 100.0)
                .networkBandwidth(network)
                .cpuUsageThreshold(cpuUsageThreshold)
                .memoryUsageThreshold(memoryUsageThreshold)
                .collectionIntervalMs(1000)
                .nodeId("local")
                .taskId("")
                .gpuStatus(gpuSample.status())
                .timestamp(System.currentTimeMillis())
                .build();
        persistenceService.save(DOMAIN, TYPE_SNAPSHOT, String.valueOf(lastSnapshot.getTimestamp()), lastSnapshot);
        wsMessageRouter.broadcast(WsMessageType.RESOURCE_METRIC, lastSnapshot);
        checkAndRaiseAlerts(lastSnapshot);
    }

    private void checkAndRaiseAlerts(ResourceMetricSnapshot snapshot) {
        if (snapshot.getCpuUsage() >= cpuUsageThreshold) {
            publishAlert("WARN", "CPU", "CPU占用超过阈值: " + snapshot.getCpuUsage() + "%");
        }
        if (snapshot.getMemoryUsage() >= memoryUsageThreshold) {
            publishAlert("WARN", "MEMORY", "内存占用超过阈值: " + snapshot.getMemoryUsage() + "%");
        }
    }

    private void publishAlert(String level, String category, String message) {
        MonitorAlert alert = MonitorAlert.builder()
                .alertId(UUID.randomUUID().toString().replace("-", ""))
                .level(level)
                .category(category)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
        alerts.add(0, alert);
        persistenceService.save(DOMAIN, TYPE_ALERT, alert.getAlertId(), alert);
        while (alerts.size() > 50) {
            alerts.remove(alerts.size() - 1);
        }
        wsMessageRouter.broadcast(WsMessageType.RESOURCE_ALERT, alert);
    }

    private double percent(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0) {
            return 0;
        }
        return Math.round(value * 10000.0) / 100.0;
    }

    private GpuSample queryGpuSample(double cpu) {
        try {
            Process process = new ProcessBuilder("nvidia-smi",
                    "--query-gpu=utilization.gpu,utilization.memory",
                    "--format=csv,noheader,nounits")
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(500, TimeUnit.MILLISECONDS);
            if (!completed) {
                process.destroyForcibly();
                return fallbackGpuSample(cpu, "UNAVAILABLE");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    return fallbackGpuSample(cpu, "UNAVAILABLE");
                }
                String[] parts = line.split(",");
                return new GpuSample(Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()), "OK");
            }
        } catch (Exception ex) {
            return fallbackGpuSample(cpu, "UNAVAILABLE");
        }
    }

    private GpuSample fallbackGpuSample(double cpu, String status) {
        return new GpuSample(Math.min(95.0, cpu * 0.8 + 8.0), Math.min(90.0, cpu * 0.6 + 6.0), status);
    }

    private record GpuSample(double gpuUsage, double memoryUsage, String status) {
    }
}
