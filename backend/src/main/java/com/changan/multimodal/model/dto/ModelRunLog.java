package com.changan.multimodal.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class ModelRunLog {

    private final String logId;
    private final String modelId;
    private final String jobId;
    private final String status;
    private final long startedAt;
    private final long finishedAt;
    private final long durationMs;
    private final String command;
    private final String message;
}

