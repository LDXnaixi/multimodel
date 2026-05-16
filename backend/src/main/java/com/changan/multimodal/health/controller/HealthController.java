package com.changan.multimodal.health.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.health.dto.HealthStatusResponse;
import com.changan.multimodal.realtime.session.SessionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class HealthController {

    private final SessionRegistry sessionRegistry;

    @GetMapping("/health")
    public ApiResponse<HealthStatusResponse> health() {
        return ApiResponse.success(HealthStatusResponse.builder()
                .service("multimodal-test-backend")
                .status("UP")
                .websocketClients(sessionRegistry.count())
                .build());
    }
}
