package com.changan.multimodal.realtime.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WsDebugSessionView {

    private final String clientId;
    private final boolean open;
    private final boolean monitorSession;
    private final String subscriptionTaskId;
    private final long connectedAt;
    private final long lastSeenAt;
    private final String remoteAddress;
    private final String userAgent;
}
