package com.changan.multimodal.task.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@Jacksonized
public class TaskInstance {

    private final String taskId;
    private final String taskName;
    private final String scenarioDescription;
    private final TaskStatus status;
    private final List<TaskNode> nodes;
    private final long createdAt;
    private final long updatedAt;

    public TaskInstance withStatus(TaskStatus newStatus) {
        return this.toBuilder()
                .status(newStatus)
                .updatedAt(Instant.now().toEpochMilli())
                .build();
    }

    public TaskInstance withNodes(List<TaskNode> newNodes) {
        return this.toBuilder()
                .nodes(newNodes)
                .updatedAt(Instant.now().toEpochMilli())
                .build();
    }
}
