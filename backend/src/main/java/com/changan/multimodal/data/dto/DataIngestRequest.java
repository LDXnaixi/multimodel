package com.changan.multimodal.data.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DataIngestRequest {

    @NotBlank(message = "datasetName不能为空")
    private String datasetName;

    @Valid
    @NotEmpty(message = "assets不能为空")
    private List<DataAsset> assets;

    private Map<String, Object> filterRules;
}
