package com.changan.multimodal.realtime.service;

import com.changan.multimodal.realtime.dto.WsEnvelope;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.session.ClientSession;
import com.changan.multimodal.realtime.session.SessionRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WsMessageRouter {

    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void onConnected(String clientId,
                            org.springframework.web.socket.WebSocketSession session,
                            String remoteAddress,
                            String userAgent) {
        long now = Instant.now().toEpochMilli();
        ClientSession clientSession = ClientSession.builder()
                .clientId(clientId)
                .session(session)
                .connectedAt(now)
                .remoteAddress(remoteAddress)
                .userAgent(userAgent)
                .lastSeenAt(now)
                .build();
        sessionRegistry.register(clientSession);
        publishMonitorEvent("CONNECTED", clientSession, null, null, null);
    }

    public void onDisconnected(String clientId) {
        sessionRegistry.find(clientId).ifPresent(clientSession ->
                publishMonitorEvent("DISCONNECTED", clientSession, null, null, null));
        sessionRegistry.remove(clientId);
    }

    public void handleMessage(String clientId, String text) throws IOException {
        JsonNode root = objectMapper.readTree(text);
        String type = Optional.ofNullable(root.get("type")).map(JsonNode::asText).orElseThrow(() -> new IllegalArgumentException("缺少type字段"));
        ClientSession clientSession = sessionRegistry.find(clientId)
                .orElseThrow(() -> new IllegalArgumentException("客户端会话不存在"));
        clientSession.setLastSeenAt(Instant.now().toEpochMilli());
        publishMonitorEvent("CLIENT_MESSAGE", clientSession, type, root.path("requestId").asText(null), root);
        if (WsMessageType.CLIENT_MONITOR_SUBSCRIBE.equals(type)) {
            clientSession.setMonitorSession(true);
            send(clientSession, WsEnvelope.<Object>builder()
                    .type(WsMessageType.SERVER_ACK)
                    .timestamp(Instant.now().toEpochMilli())
                    .requestId(root.path("requestId").asText())
                    .payload(new AckPayload("MONITOR_SUBSCRIBED", clientSession.getSubscriptionTaskId(), type))
                    .build());
            return;
        }
        if (WsMessageType.CLIENT_SUBSCRIBE.equals(type)) {
            JsonNode payload = root.get("payload");
            String taskId = payload != null && payload.get("taskId") != null ? payload.get("taskId").asText() : null;
            clientSession.setSubscriptionTaskId(taskId);
            send(clientSession, WsEnvelope.<Object>builder()
                    .type(WsMessageType.SERVER_ACK)
                    .timestamp(Instant.now().toEpochMilli())
                    .requestId(root.path("requestId").asText())
                    .payload(new AckPayload("SUBSCRIBED", taskId, type))
                    .build());
            return;
        }
        if (WsMessageType.CLIENT_PONG.equals(type)) {
            send(clientSession, WsEnvelope.<Object>builder()
                    .type(WsMessageType.SERVER_ACK)
                    .timestamp(Instant.now().toEpochMilli())
                    .requestId(root.path("requestId").asText())
                    .payload(new AckPayload("PONG_OK", clientSession.getSubscriptionTaskId(), type))
                    .build());
            return;
        }
        send(clientSession, WsEnvelope.<Object>builder()
                .type(WsMessageType.SERVER_ACK)
                .timestamp(Instant.now().toEpochMilli())
                .requestId(root.path("requestId").asText())
                .payload(new AckPayload("RECEIVED_UNHANDLED", clientSession.getSubscriptionTaskId(), type))
                .build());
    }

    public void broadcastTaskProgress(String taskId, Object payload) {
        for (ClientSession clientSession : sessionRegistry.all()) {
            if (taskId.equals(clientSession.getSubscriptionTaskId())) {
                send(clientSession, WsEnvelope.<Object>builder()
                        .type(WsMessageType.TASK_PROGRESS)
                        .timestamp(Instant.now().toEpochMilli())
                        .requestId(taskId)
                        .payload(payload)
                        .build());
            }
        }
    }

    public void broadcast(String type, Object payload) {
        for (ClientSession clientSession : sessionRegistry.all()) {
            send(clientSession, WsEnvelope.<Object>builder()
                    .type(type)
                    .timestamp(Instant.now().toEpochMilli())
                    .requestId(type + "-" + Instant.now().toEpochMilli())
                    .payload(payload)
                    .build());
        }
    }

    public int pushToClient(String clientId, String type, String requestId, Object payload) {
        ClientSession clientSession = sessionRegistry.find(clientId)
                .orElseThrow(() -> new IllegalArgumentException("目标客户端不存在: " + clientId));
        send(clientSession, WsEnvelope.<Object>builder()
                .type(type)
                .timestamp(Instant.now().toEpochMilli())
                .requestId(resolveRequestId(requestId, type))
                .payload(payload)
                .build());
        return 1;
    }

    public int pushToAllBusinessClients(String type, String requestId, Object payload) {
        List<ClientSession> deliveredClients = new ArrayList<>();
        for (ClientSession clientSession : sessionRegistry.all()) {
            if (clientSession.isMonitorSession()) {
                continue;
            }
            send(clientSession, WsEnvelope.<Object>builder()
                    .type(type)
                    .timestamp(Instant.now().toEpochMilli())
                    .requestId(resolveRequestId(requestId, type))
                    .payload(payload)
                    .build());
            deliveredClients.add(clientSession);
        }
        return deliveredClients.size();
    }

    private void send(ClientSession clientSession, WsEnvelope<?> envelope) {
        send(clientSession, envelope, true);
    }

    private void send(ClientSession clientSession, WsEnvelope<?> envelope, boolean publishEvent) {
        try {
            if (clientSession.getSession().isOpen()) {
                clientSession.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(envelope)));
                if (publishEvent) {
                    publishMonitorEvent("SERVER_MESSAGE", clientSession, envelope.getType(), envelope.getRequestId(), envelope);
                }
            }
        } catch (IOException ex) {
            sessionRegistry.remove(clientSession.getClientId());
        }
    }

    private void publishMonitorEvent(String eventKind,
                                     ClientSession sourceClient,
                                     String messageType,
                                     String requestId,
                                     Object payload) {
        long now = Instant.now().toEpochMilli();
        MonitorEventPayload eventPayload = new MonitorEventPayload(
                eventKind,
                sourceClient.getClientId(),
                sourceClient.getSubscriptionTaskId(),
                sourceClient.isMonitorSession(),
                sourceClient.getConnectedAt(),
                sourceClient.getLastSeenAt(),
                sourceClient.getRemoteAddress(),
                sourceClient.getUserAgent(),
                messageType,
                requestId,
                payload,
                now
        );
        for (ClientSession clientSession : sessionRegistry.all()) {
            if (!clientSession.isMonitorSession() || !clientSession.getSession().isOpen()) {
                continue;
            }
            try {
                clientSession.getSession().sendMessage(new TextMessage(objectMapper.writeValueAsString(WsEnvelope.<Object>builder()
                        .type(WsMessageType.MONITOR_EVENT)
                        .timestamp(now)
                        .requestId("monitor-" + now)
                        .payload(eventPayload)
                        .build())));
            } catch (IOException ex) {
                sessionRegistry.remove(clientSession.getClientId());
            }
        }
    }

    private String resolveRequestId(String requestId, String type) {
        return requestId == null || requestId.isBlank() ? type + "-" + Instant.now().toEpochMilli() : requestId;
    }

    private record AckPayload(String status, String taskId, String originalType) {
    }

    private record MonitorEventPayload(String eventKind,
                                       String clientId,
                                       String subscriptionTaskId,
                                       boolean monitorSession,
                                       long connectedAt,
                                       long lastSeenAt,
                                       String remoteAddress,
                                       String userAgent,
                                       String messageType,
                                       String requestId,
                                       Object payload,
                                       long observedAt) {
    }
}
