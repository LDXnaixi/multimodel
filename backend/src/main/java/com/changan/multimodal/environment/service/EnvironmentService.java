package com.changan.multimodal.environment.service;

import com.changan.multimodal.common.persistence.DemoPersistenceService;
import com.changan.multimodal.environment.dto.EnvironmentReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final DemoPersistenceService persistenceService;

    public EnvironmentReport capture() {
        String javaVersion = System.getProperty("java.version", "unknown");
        String osName = System.getProperty("os.name", "unknown");
        EnvironmentReport report = EnvironmentReport.builder()
                .reportId(UUID.randomUUID().toString().replace("-", ""))
                .capturedAt(Instant.now().toEpochMilli())
                .system(Map.of(
                        "osName", osName,
                        "osArch", System.getProperty("os.arch", "unknown"),
                        "osVersion", System.getProperty("os.version", "unknown"),
                        "availableProcessors", String.valueOf(Runtime.getRuntime().availableProcessors())
                ))
                .runtime(Map.of(
                        "javaVersion", javaVersion,
                        "javaVendor", System.getProperty("java.vendor", "unknown"),
                        "springProfile", System.getProperty("spring.profiles.active", "default"),
                        "userTimezone", System.getProperty("user.timezone", "unknown")
                ))
                .dependencies(List.of(
                        "Spring Boot 3.3.5",
                        "Vue 3",
                        "MySQL 8.x",
                        "本地 Python 模型进程适配器"
                ))
                .conflicts(detectConflicts(javaVersion))
                .recommendations(List.of(
                        "生产环境建议固定 JDK 17 LTS",
                        "模型服务脚本与权重建议按 modelId/version 分目录管理",
                        "GPU 监控命令不可用时需安装厂商驱动或配置适配命令"
                ))
                .consistent(javaVersion.startsWith("17") || javaVersion.startsWith("18") || javaVersion.startsWith("19") || javaVersion.startsWith("20") || javaVersion.startsWith("21"))
                .build();
        return persistenceService.save("ENVIRONMENT", "REPORT", report.getReportId(), report);
    }

    public List<EnvironmentReport> listReports() {
        return persistenceService.findAll("ENVIRONMENT", "REPORT", EnvironmentReport.class);
    }

    private List<String> detectConflicts(String javaVersion) {
        if (javaVersion.startsWith("1.") || javaVersion.startsWith("8") || javaVersion.startsWith("11")) {
            return List.of("当前 Java 版本低于项目编译目标 Java 17");
        }
        return List.of();
    }
}

