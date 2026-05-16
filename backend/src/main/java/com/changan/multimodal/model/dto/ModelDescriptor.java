package com.changan.multimodal.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ModelDescriptor {

    private final String modelId;
    private final String modelName;
    private final String version;
    private final String algorithmType;
    private final String deploymentStatus;
    private final List<String> supportedModalities;
    private final String todo;
}
