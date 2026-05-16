package com.changan.multimodal.inference.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class InferenceInput {

    @NotBlank(message = "inputId不能为空")
    private String inputId;

    @NotBlank(message = "sourceUri不能为空")
    private String sourceUri;

    private Map<String, Object> attributes;
}
