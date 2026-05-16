package com.changan.multimodal.inference.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class InferenceRequest {

    @NotBlank(message = "modelId不能为空")
    private String modelId;

    @NotBlank(message = "modality不能为空")
    private String modality;

    @Valid
    @NotEmpty(message = "inputs不能为空")
    private List<InferenceInput> inputs;

    private List<String> requestedMetrics;

    private Map<String, Object> options;
}
