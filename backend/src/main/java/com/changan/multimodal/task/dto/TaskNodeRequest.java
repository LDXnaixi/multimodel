package com.changan.multimodal.task.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class TaskNodeRequest {

    private String nodeId;
    private String nodeName;
    private String nodeType;
    private Integer priority;
    private Double resourceRatio;
    private Map<String, Object> parameters;
}
