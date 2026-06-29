package com.changan.multimodal.monitor.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class ResourceMetricSnapshot {

    private final double cpuUsage;
    private final double memoryUsage;
    private final double heapUsage;
    private final double diskUsage;
    private final double networkThroughput;
    private final double gpuUsage;
    private final double gpuMemoryUsage;
    private final double diskReadRate;
    private final double diskWriteRate;
    private final double networkBandwidth;
    private final int cpuUsageThreshold;
    private final int memoryUsageThreshold;
    private final long collectionIntervalMs;
    private final String nodeId;
    private final String taskId;
    private final String gpuStatus;
    private final long timestamp;
}
