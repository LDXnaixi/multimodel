package com.changan.multimodal.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateTaskRequest {

    @NotBlank(message = "任务名称不能为空")
    private String taskName;

    private String scenarioDescription;

    private List<TaskNodeRequest> nodes;
}
