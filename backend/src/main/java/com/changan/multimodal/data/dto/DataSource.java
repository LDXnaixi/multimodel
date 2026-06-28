package com.changan.multimodal.data.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DataSource {
    private String sourceId;
    private String name;
    private String type;
    private String config;
    private String status;
    private Instant connectedAt;
}
