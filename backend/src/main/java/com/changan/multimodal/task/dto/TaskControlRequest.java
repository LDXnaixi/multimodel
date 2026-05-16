package com.changan.multimodal.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskControlRequest {

    @NotNull(message = "控制动作不能为空")
    private TaskControlAction action;
}
