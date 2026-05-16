package com.changan.multimodal.model.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.model.dto.ModelDescriptor;
import com.changan.multimodal.model.service.ModelRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelRegistryService modelRegistryService;

    @GetMapping
    public ApiResponse<List<ModelDescriptor>> list() {
        return ApiResponse.success(modelRegistryService.listModels());
    }
}
