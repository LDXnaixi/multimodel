package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DataProcessingRequest {
    private String sampleId;
    private String taskType;
    private List<ProcessingStep> steps;
}
