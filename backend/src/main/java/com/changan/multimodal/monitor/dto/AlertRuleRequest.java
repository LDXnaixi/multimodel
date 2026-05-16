package com.changan.multimodal.monitor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertRuleRequest {

    @Min(1)
    @Max(100)
    private int cpuUsageThreshold = 85;

    @Min(1)
    @Max(100)
    private int memoryUsageThreshold = 90;
}
