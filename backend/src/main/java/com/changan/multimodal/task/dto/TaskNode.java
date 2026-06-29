package com.changan.multimodal.task.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@Jacksonized
public class TaskNode {

    private final String nodeId;
    private final String nodeName;
    private final String nodeType;
    private final TaskNodeStatus status;
    private final Integer priority;
    private final Double resourceRatio;
    private final List<String> dependencies;
    private final String conditionExpression;
    private final String failureStrategy;
    private final Map<String, Object> parameters;
    private final String todo;

    public TaskNode withStatus(TaskNodeStatus newStatus) {
        return this.toBuilder().status(newStatus).build();
    }
}
