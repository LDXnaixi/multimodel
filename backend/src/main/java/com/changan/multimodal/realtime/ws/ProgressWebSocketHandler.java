package com.changan.multimodal.realtime.ws;

import com.changan.multimodal.realtime.service.WsMessageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProgressWebSocketHandler extends TextWebSocketHandler {

    private final WsMessageRouter wsMessageRouter;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String clientId = resolveClientId(session);
        session.getAttributes().put("clientId", clientId);
        wsMessageRouter.onConnected(clientId, session, resolveRemoteAddress(session), session.getHandshakeHeaders().getFirst("User-Agent"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        wsMessageRouter.handleMessage(resolveClientId(session), message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        wsMessageRouter.onDisconnected(resolveClientId(session));
    }

    private String resolveClientId(WebSocketSession session) {
        Object existing = session.getAttributes().get("clientId");
        return existing == null ? UUID.randomUUID().toString().replace("-", "") : existing.toString();
    }

    private String resolveRemoteAddress(WebSocketSession session) {
        return session.getRemoteAddress() == null ? "unknown" : session.getRemoteAddress().toString();
    }
}
