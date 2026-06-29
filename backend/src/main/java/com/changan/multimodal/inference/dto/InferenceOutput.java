package com.changan.multimodal.inference.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Builder
@Jacksonized
public class InferenceOutput {

    private final String inputId;
    private final String label;
    private final double confidence;
    private final Map<String, Object> extra;
}
