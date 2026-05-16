package com.changan.multimodal.monitor.service;

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
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class ResourceMonitorService {

    private final WsMessageRouter wsMessageRouter;
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
            .cpuUsageThreshold(cpuUsageThreshold)
            .memoryUsageThreshold(memoryUsageThreshold)
            .timestamp(System.currentTimeMillis())
            .build();

    public ResourceMetricSnapshot currentSnapshot() {
        return lastSnapshot;
    }

    public List<MonitorAlert> listAlerts() {
        return List.copyOf(alerts);
    }

    public void updateThresholds(AlertRuleRequest request) {
        this.cpuUsageThreshold = request.getCpuUsageThreshold();
        this.memoryUsageThreshold = request.getMemoryUsageThreshold();
        collectSnapshot();
    }

    @Scheduled(fixedDelay = 5000L)
    public void collectSnapshot() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        Runtime runtime = Runtime.getRuntime();
        File root = new File(".");
        double cpu = percent(operatingSystemMXBean.getCpuLoad());
        double memory = percent(1 - ((double) operatingSystemMXBean.getFreeMemorySize() / operatingSystemMXBean.getTotalMemorySize()));
        double heap = percent((double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory());
        double disk = percent(1 - ((double) root.getFreeSpace() / root.getTotalSpace()));
        double gpu = Math.min(95.0, cpu * 0.8 + 8.0);
        double network = Math.round((cpu * 1.37 + memory * 0.92) * 100.0) / 100.0;
        lastSnapshot = ResourceMetricSnapshot.builder()
                .cpuUsage(cpu)
                .memoryUsage(memory)
                .heapUsage(heap)
                .diskUsage(disk)
                .networkThroughput(network)
                .gpuUsage(gpu)
                .cpuUsageThreshold(cpuUsageThreshold)
                .memoryUsageThreshold(memoryUsageThreshold)
                .timestamp(System.currentTimeMillis())
                .build();
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
}
