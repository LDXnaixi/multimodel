package com.changan.multimodal.monitor.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResourceMetricSnapshot {

    private final double cpuUsage;
    private final double memoryUsage;
    private final double heapUsage;
    private final double diskUsage;
    private final double networkThroughput;
    private final double gpuUsage;
    private final int cpuUsageThreshold;
    private final int memoryUsageThreshold;
    private final long timestamp;
}
