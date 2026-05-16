package com.changan.multimodal.inference.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class InferenceOutput {

    private final String inputId;
    private final String label;
    private final double confidence;
    private final Map<String, Object> extra;
}
