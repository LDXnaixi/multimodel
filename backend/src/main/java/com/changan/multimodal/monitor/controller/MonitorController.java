package com.changan.multimodal.monitor.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.monitor.dto.AlertRuleRequest;
import com.changan.multimodal.monitor.dto.MonitorAlert;
import com.changan.multimodal.monitor.dto.ResourceMetricSnapshot;
import com.changan.multimodal.monitor.service.ResourceMonitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final ResourceMonitorService resourceMonitorService;

    @GetMapping("/metrics")
    public ApiResponse<ResourceMetricSnapshot> metrics() {
        return ApiResponse.success(resourceMonitorService.currentSnapshot());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<MonitorAlert>> alerts() {
        return ApiResponse.success(resourceMonitorService.listAlerts());
    }

    @PostMapping("/thresholds")
    public ApiResponse<ResourceMetricSnapshot> updateThreshold(@Valid @RequestBody AlertRuleRequest request) {
        resourceMonitorService.updateThresholds(request);
        return ApiResponse.success(resourceMonitorService.currentSnapshot());
    }
}
