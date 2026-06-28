package com.changan.multimodal.data.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProcessingStep {
    private String type;
    private Map<String, Object> config;
}
