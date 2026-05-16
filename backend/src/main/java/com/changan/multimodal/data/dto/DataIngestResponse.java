package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DataIngestResponse {

    private final String datasetId;
    private final String datasetName;
    private final int assetCount;
    private final List<String> supportedModalities;
    private final String version;
    private final String status;
}
