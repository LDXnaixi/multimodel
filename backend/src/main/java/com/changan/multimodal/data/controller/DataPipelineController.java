package com.changan.multimodal.data.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.data.dto.DataIngestRequest;
import com.changan.multimodal.data.dto.DataIngestResponse;
import com.changan.multimodal.data.dto.DataPipelineRequest;
import com.changan.multimodal.data.dto.DataPipelineResponse;
import com.changan.multimodal.data.service.DataPipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataPipelineController {

    private final DataPipelineService dataPipelineService;

    @PostMapping("/datasets/register")
    public ApiResponse<DataIngestResponse> register(@Valid @RequestBody DataIngestRequest request) {
        return ApiResponse.success(dataPipelineService.registerDataset(request));
    }

    @PostMapping("/pipelines/run")
    public ApiResponse<DataPipelineResponse> run(@Valid @RequestBody DataPipelineRequest request) {
        return ApiResponse.success(dataPipelineService.runPipeline(request));
    }
}
