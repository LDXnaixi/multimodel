package com.changan.multimodal.task.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Builder
@Jacksonized
public class TaskTemplateView {

    private final String templateId;
    private final String templateName;
    private final String description;
    private final List<TaskNode> nodes;
    private final long createdAt;
}

