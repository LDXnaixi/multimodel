package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DataProcessingResponse {
    private String taskId;
    private String status;
    private List<String> processedSampleIds;
    private Map<String, Object> results;
}
