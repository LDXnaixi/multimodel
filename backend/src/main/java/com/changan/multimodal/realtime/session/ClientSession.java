package com.changan.multimodal.realtime.session;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

@Getter
@Builder
@AllArgsConstructor
public class ClientSession {

    private final String clientId;
    private final WebSocketSession session;
    private final long connectedAt;
    private final String remoteAddress;
    private final String userAgent;

    @Setter
    private volatile long lastSeenAt;

    @Setter
    private volatile String subscriptionTaskId;

    @Setter
    private volatile boolean monitorSession;
}
