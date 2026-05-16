package com.changan.multimodal.data.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DataAsset {

    @NotBlank(message = "assetId不能为空")
    private String assetId;

    @NotBlank(message = "uri不能为空")
    private String uri;

    private String modality;

    private List<String> tags;
}
