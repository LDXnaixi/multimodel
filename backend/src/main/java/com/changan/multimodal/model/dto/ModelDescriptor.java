package com.changan.multimodal.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@Jacksonized
public class ModelDescriptor {

    private final String modelId;
    private final String modelName;
    private final String version;
    private final String algorithmType;
    private final String deploymentStatus;
    private final List<String> supportedModalities;
    private final String todo;
    private final Integer invocationCount;
    private final Double averageLatency;
    private final String modelCategory;
    private final List<String> availableMetrics;
    private final Boolean isCustom;
    private final String runtimeCommand;
    private final String packageUri;
    private final String activeVersion;
    private final String adapterType;
    private final String adapterStatus;
    private final Map<String, Object> adapterConfig;
    private final List<String> datasetFormats;
    private final Map<String, Object> outputSchema;
    private final long updatedAt;
}
