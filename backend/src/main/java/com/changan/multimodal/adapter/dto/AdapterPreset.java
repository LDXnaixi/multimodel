package com.changan.multimodal.adapter.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@Jacksonized
public class AdapterPreset {
    private final String presetId;
    private final String displayName;
    private final String modelFamily;
    private final String taskType;
    private final String adapterType;
    private final String adapterStatus;
    private final List<String> supportedModels;
    private final List<String> supportedModalities;
    private final List<String> datasetFormats;
    private final List<String> metrics;
    private final List<String> requiredArtifacts;
    private final Map<String, Object> inputSchema;
    private final Map<String, Object> outputSchema;
    private final Map<String, Object> defaultConfig;
    private final List<Map<String, Object>> configurableFields;
    private final String runnerKind;
    private final String runnerTemplate;
    private final List<String> officialDocUrls;
    private final String compatibilityNotes;
    private final boolean custom;
    private final long updatedAt;
}
