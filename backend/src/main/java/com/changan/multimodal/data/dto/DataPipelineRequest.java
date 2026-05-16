package com.changan.multimodal.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataPipelineRequest {

    @NotBlank(message = "datasetId不能为空")
    private String datasetId;

    @Valid
    @NotEmpty(message = "operations不能为空")
    private List<DataPipelineOperation> operations;
}
