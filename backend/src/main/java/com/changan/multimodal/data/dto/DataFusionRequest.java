package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DataFusionRequest {
    private List<String> sampleIds;
    private String fusionStrategy;
    private Map<String, Object> alignmentConfig;
}
