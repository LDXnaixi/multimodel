package com.changan.multimodal.inference.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InferenceResponse {

    private final String jobId;
    private final String modelId;
    private final String modality;
    private final long durationMs;
    private final List<InferenceOutput> outputs;
    private final List<EvaluationMetric> metrics;
    private final String status;
}
