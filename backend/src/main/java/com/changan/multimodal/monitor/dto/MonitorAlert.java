package com.changan.multimodal.monitor.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonitorAlert {

    private final String alertId;
    private final String level;
    private final String category;
    private final String message;
    private final long timestamp;
}
