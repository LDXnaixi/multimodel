package com.changan.multimodal.task.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RerunRequest {

    private String mode = "FULL_CHAIN";
    private String nodeId;
}

