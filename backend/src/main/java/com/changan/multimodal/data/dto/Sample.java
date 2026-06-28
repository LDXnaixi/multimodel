package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Sample {
    private String sampleId;
    private String name;
    private String dataType;
    private String sourceUri;
    private List<String> tags;
    private Map<String, Object> metadata;
    private String version;
    private Instant createdAt;
    private Instant updatedAt;
}
