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
    private String datasetId;
    private String name;
    private String dataType;
    private String contentUrl;
    private String imageRelativePath;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String sha256;
    private String labelContentUrl;
    private String labelRelativePath;
    private String labelOriginalName;
    private String labelContentType;
    private Long labelFileSize;
    private String labelSha256;
    private List<String> tags;
    private Map<String, Object> metadata;
    private String version;
    private Instant createdAt;
    private Instant updatedAt;
}
