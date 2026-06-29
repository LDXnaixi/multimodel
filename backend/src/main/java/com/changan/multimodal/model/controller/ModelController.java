package com.changan.multimodal.model.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.model.dto.AnalyzeModelRequest;
import com.changan.multimodal.model.dto.ModelDescriptor;
import com.changan.multimodal.model.dto.ModelLifecycleRequest;
import com.changan.multimodal.model.dto.ModelRunLog;
import com.changan.multimodal.model.service.ModelArtifactService;
import com.changan.multimodal.model.service.ModelRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelRegistryService modelRegistryService;
    private final ModelArtifactService modelArtifactService;

    @GetMapping
    public ApiResponse<List<ModelDescriptor>> list() {
        return ApiResponse.success(modelRegistryService.listModels());
    }

    @PostMapping("/analyze")
    public ApiResponse<ModelDescriptor> analyze(@RequestBody AnalyzeModelRequest request) {
        return ApiResponse.success(modelRegistryService.analyzeModel(request.getFileName(), request.getFileSize()));
    }

    @PostMapping
    public ApiResponse<ModelDescriptor> register(@RequestBody ModelLifecycleRequest request) {
        return ApiResponse.success(modelRegistryService.register(request));
    }

    @PostMapping("/artifacts/upload")
    public ApiResponse<Map<String, Object>> uploadArtifact(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(value = "relativePaths", required = false) List<String> relativePaths,
            @RequestParam(value = "kind", defaultValue = "file") String kind) {
        return ApiResponse.success(modelArtifactService.store(files, relativePaths, kind));
    }

    @PostMapping("/{modelId}/status")
    public ApiResponse<ModelDescriptor> changeStatus(@PathVariable String modelId, @RequestBody Map<String, String> request) {
        return ApiResponse.success(modelRegistryService.changeStatus(modelId, request.getOrDefault("status", "RUNNING")));
    }

    @PostMapping("/{modelId}/rollback")
    public ApiResponse<ModelDescriptor> rollback(@PathVariable String modelId, @RequestBody Map<String, String> request) {
        return ApiResponse.success(modelRegistryService.rollback(modelId, request.getOrDefault("version", "1.0.0")));
    }

    @GetMapping("/run-logs")
    public ApiResponse<List<ModelRunLog>> runLogs() {
        return ApiResponse.success(modelRegistryService.listRunLogs());
    }
}
