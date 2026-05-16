package com.changan.multimodal.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class DataPipelineOperation {

    @NotBlank(message = "operation不能为空")
    private String operation;

    private Map<String, Object> parameters;
}
