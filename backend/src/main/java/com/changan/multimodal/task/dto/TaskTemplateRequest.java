package com.changan.multimodal.task.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskTemplateRequest {

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    private String description;

    @Valid
    private List<TaskNodeRequest> nodes;
}

