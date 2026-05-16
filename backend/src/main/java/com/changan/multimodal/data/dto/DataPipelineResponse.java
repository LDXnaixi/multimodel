package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DataPipelineResponse {

    private final String pipelineId;
    private final String datasetId;
    private final List<String> executedOperations;
    private final List<String> generatedAssets;
    private final String status;
}
