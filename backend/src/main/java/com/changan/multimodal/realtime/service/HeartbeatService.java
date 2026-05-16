package com.changan.multimodal.realtime.service;

import com.changan.multimodal.realtime.dto.WsEnvelope;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.session.ClientSession;
import com.changan.multimodal.realtime.session.SessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private static final long SESSION_EXPIRE_MILLIS = 30_000L;

    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 10_000L)
    public void sendPingAndCleanExpiredSessions() {
        long now = Instant.now().toEpochMilli();
        for (ClientSession clientSession : sessionRegistry.all()) {
            try {
                if (!clientSession.getSession().isOpen() || now - clientSession.getLastSeenAt() > SESSION_EXPIRE_MILLIS) {
                    clientSession.getSession().close();
                    sessionRegistry.remove(clientSession.getClientId());
                    continue;
                }
                WsEnvelope<Object> ping = WsEnvelope.<Object>builder()
                        .type(WsMessageType.SERVER_PING)
                        .timestamp(now)
                        .requestId("ping-" + now)
                        .payload(null)
                        .build();
                clientSession.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(ping)));
            } catch (IOException ex) {
                log.warn("Heartbeat send failed for clientId={}", clientSession.getClientId(), ex);
                sessionRegistry.remove(clientSession.getClientId());
            }
        }
    }
}
