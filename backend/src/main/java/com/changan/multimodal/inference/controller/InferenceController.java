package com.changan.multimodal.inference.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.inference.dto.InferenceRequest;
import com.changan.multimodal.inference.dto.InferenceResponse;
import com.changan.multimodal.inference.service.ModelInferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inference")
@RequiredArgsConstructor
public class InferenceController {

    private final ModelInferenceService modelInferenceService;

    @PostMapping("/run")
    public ApiResponse<InferenceResponse> run(@Valid @RequestBody InferenceRequest request) {
        return ApiResponse.success(modelInferenceService.runInference(request));
    }
}
