package com.changan.multimodal.task.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TaskProgressEvent {

    private final String taskId;
    private final String taskStatus;
    private final String nodeId;
    private final String nodeStatus;
    private final int progress;
    private final String message;
}
