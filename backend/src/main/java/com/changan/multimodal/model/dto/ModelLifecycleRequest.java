package com.changan.multimodal.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ModelLifecycleRequest {

    private String modelId;
    private String modelName;
    private String version;
    private String algorithmType;
    private String modelCategory;
    private List<String> supportedModalities;
    private List<String> availableMetrics;
    private String runtimeCommand;
    private String packageUri;
    private String adapterType;
    private String adapterStatus;
    private Map<String, Object> adapterConfig;
    private List<String> datasetFormats;
    private Map<String, Object> metadata;
}
