package com.changan.multimodal.environment.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.environment.dto.EnvironmentReport;
import com.changan.multimodal.environment.service.EnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environment")
@RequiredArgsConstructor
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @PostMapping("/capture")
    public ApiResponse<EnvironmentReport> capture() {
        return ApiResponse.success(environmentService.capture());
    }

    @GetMapping("/reports")
    public ApiResponse<List<EnvironmentReport>> reports() {
        return ApiResponse.success(environmentService.listReports());
    }
}

