package com.changan.multimodal.data.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataIngestConfig {
    private String sourceId;
    private List<FilterRule> filterRules;
    private List<TagMapping> tagMappings;
    private String datasetId;
}
