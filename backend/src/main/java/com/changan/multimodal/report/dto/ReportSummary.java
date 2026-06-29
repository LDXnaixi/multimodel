package com.changan.multimodal.report.dto;

import com.changan.multimodal.monitor.dto.ResourceMetricSnapshot;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Jacksonized
public class ReportSummary {

    private final String taskId;
    private final String taskName;
    private final String taskStatus;
    private final int progress;
    private final List<Map<String, Object>> nodes;
    private final List<Map<String, Object>> metrics;
    private final ResourceMetricSnapshot resourceSnapshot;
    private final String conclusion;
}
