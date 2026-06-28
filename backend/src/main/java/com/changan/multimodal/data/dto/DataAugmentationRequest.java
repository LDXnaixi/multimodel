package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataAugmentationRequest {
    private List<String> sampleIds;
    private String dataType;
    private List<AugmentationConfig> configs;
    private int augmentationFactor;
}
