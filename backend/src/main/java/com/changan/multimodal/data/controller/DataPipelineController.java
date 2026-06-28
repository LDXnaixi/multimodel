package com.changan.multimodal.data.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.data.dto.*;
import com.changan.multimodal.data.service.DataPipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/datasources")
    public ApiResponse<List<DataSource>> listDataSources() {
        return ApiResponse.success(dataPipelineService.listDataSources());
    }

    @PostMapping("/datasources")
    public ApiResponse<DataSource> addDataSource(@Valid @RequestBody DataSource dataSource) {
        return ApiResponse.success(dataPipelineService.addDataSource(dataSource));
    }

    @PostMapping("/samples/search")
    public ApiResponse<List<Sample>> searchSamples(@Valid @RequestBody SampleSearchRequest request) {
        return ApiResponse.success(dataPipelineService.searchSamples(request));
    }

    @PostMapping("/samples")
    public ApiResponse<Sample> addSample(@Valid @RequestBody Sample sample) {
        return ApiResponse.success(dataPipelineService.addSample(sample));
    }

    @PostMapping("/processing")
    public ApiResponse<DataProcessingResponse> processData(@Valid @RequestBody DataProcessingRequest request) {
        return ApiResponse.success(dataPipelineService.processData(request));
    }

    @PostMapping("/augmentation")
    public ApiResponse<List<String>> augmentData(@Valid @RequestBody DataAugmentationRequest request) {
        return ApiResponse.success(dataPipelineService.augmentData(request));
    }

    @PostMapping("/fusion")
    public ApiResponse<List<String>> fuseData(@Valid @RequestBody DataFusionRequest request) {
        return ApiResponse.success(dataPipelineService.fuseData(request));
    }

    @PostMapping("/scenarios/generate")
    public ApiResponse<List<String>> generateScenario(@Valid @RequestBody ScenarioGenerationRequest request) {
        return ApiResponse.success(dataPipelineService.generateScenario(request));
    }
}
