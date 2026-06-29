package com.changan.multimodal.adapter.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AdapterPresetRequest {
    private String presetId;
    private String displayName;
    private String modelFamily;
    private String taskType;
    private String adapterType;
    private String adapterStatus;
    private List<String> supportedModels;
    private List<String> supportedModalities;
    private List<String> datasetFormats;
    private List<String> metrics;
    private List<String> requiredArtifacts;
    private Map<String, Object> inputSchema;
    private Map<String, Object> outputSchema;
    private Map<String, Object> defaultConfig;
    private List<Map<String, Object>> configurableFields;
    private String runnerKind;
    private String runnerTemplate;
    private List<String> officialDocUrls;
    private String compatibilityNotes;
    private Boolean custom;
}
