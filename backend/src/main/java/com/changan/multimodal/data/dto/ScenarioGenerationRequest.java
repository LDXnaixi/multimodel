package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ScenarioGenerationRequest {
    private String userDescription;
    private String baseDatasetId;
    private int targetCount;
    private Map<String, Object> constraints;
}
