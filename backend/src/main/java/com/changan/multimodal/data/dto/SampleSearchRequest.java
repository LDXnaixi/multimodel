package com.changan.multimodal.data.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class SampleSearchRequest {
    private String datasetId;
    private String keyword;
    private String eventType;
    private Instant startTime;
    private Instant endTime;
    private List<String> tags;
    private String dataType;
}
