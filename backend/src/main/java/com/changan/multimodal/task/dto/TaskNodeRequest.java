package com.changan.multimodal.task.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class TaskNodeRequest {

    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Integer priority;
    private Double resourceRatio;
    private List<String> dependencies;
    private String conditionExpression;
    private String failureStrategy;
    private Map<String, Object> parameters;
}
