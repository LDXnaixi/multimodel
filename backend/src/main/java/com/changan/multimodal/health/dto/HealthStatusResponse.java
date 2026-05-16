package com.changan.multimodal.health.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class HealthStatusResponse {

    private final String service;
    private final String status;
    private final int websocketClients;
}
