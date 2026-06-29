package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.time.Instant;

@Getter
@Builder
@Jacksonized
public class DataIngestResponse {

    private final String datasetId;
    private final String datasetName;
    private final int assetCount;
    private final List<String> supportedModalities;
    private final String version;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;
}
