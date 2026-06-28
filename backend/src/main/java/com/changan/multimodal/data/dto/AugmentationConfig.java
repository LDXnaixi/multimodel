package com.changan.multimodal.data.dto;

import lombok.Data;

import java.util.Map;

@Data
public class AugmentationConfig {
    private String method;
    private Map<String, Object> parameters;
}
