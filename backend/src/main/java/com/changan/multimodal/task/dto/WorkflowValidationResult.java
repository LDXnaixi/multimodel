package com.changan.multimodal.task.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class WorkflowValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> executionOrder;
}

