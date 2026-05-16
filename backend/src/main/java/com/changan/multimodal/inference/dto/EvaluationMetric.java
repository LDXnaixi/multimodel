package com.changan.multimodal.inference.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EvaluationMetric {

    private final String name;
    private final double value;
    private final String unit;
    private final String description;
}
