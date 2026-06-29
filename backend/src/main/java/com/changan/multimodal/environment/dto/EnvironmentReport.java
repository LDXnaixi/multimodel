package com.changan.multimodal.environment.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@Jacksonized
public class EnvironmentReport {

    private final String reportId;
    private final long capturedAt;
    private final Map<String, String> system;
    private final Map<String, String> runtime;
    private final List<String> dependencies;
    private final List<String> conflicts;
    private final List<String> recommendations;
    private final boolean consistent;
}

