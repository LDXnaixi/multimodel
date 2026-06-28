package com.changan.multimodal.model.dto;

import lombok.Data;

@Data
public class AnalyzeModelRequest {
    private String fileName;
    private long fileSize;
    private String lastModified;
}
