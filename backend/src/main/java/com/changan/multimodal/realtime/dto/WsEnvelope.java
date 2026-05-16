package com.changan.multimodal.realtime.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WsEnvelope<T> {

    private final String type;
    private final long timestamp;
    private final String requestId;
    private final T payload;
}
