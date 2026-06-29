package com.changan.multimodal.inference.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class EvaluationMetric {

    private final String name;
    private final double value;
    private final String unit;
    private final String description;
}
