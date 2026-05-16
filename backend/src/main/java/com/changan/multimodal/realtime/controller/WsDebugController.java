package com.changan.multimodal.realtime.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.realtime.dto.WsDebugSendRequest;
import com.changan.multimodal.realtime.dto.WsDebugSessionView;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import com.changan.multimodal.realtime.session.ClientSession;
import com.changan.multimodal.realtime.session.SessionRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ws-debug")
@RequiredArgsConstructor
public class WsDebugController {

    private final SessionRegistry sessionRegistry;
    private final WsMessageRouter wsMessageRouter;

    @GetMapping("/sessions")
    public ApiResponse<List<WsDebugSessionView>> sessions() {
        return ApiResponse.success(sessionRegistry.all().stream()
                .map(this::toView)
                .toList());
    }

    @PostMapping("/send")
    public ApiResponse<Map<String, Object>> send(@Valid @RequestBody WsDebugSendRequest request) {
        if (!request.isBroadcast() && !StringUtils.hasText(request.getClientId())) {
            throw new IllegalArgumentException("broadcast=false 时必须提供 clientId");
        }
        int delivered = request.isBroadcast()
                ? wsMessageRouter.pushToAllBusinessClients(request.getType(), request.getRequestId(), request.getPayload())
                : wsMessageRouter.pushToClient(request.getClientId(), request.getType(), request.getRequestId(), request.getPayload());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("broadcast", request.isBroadcast());
        result.put("targetClientId", request.getClientId());
        result.put("type", request.getType());
        result.put("requestId", request.getRequestId());
        result.put("delivered", delivered);
        return ApiResponse.success(result);
    }

    private WsDebugSessionView toView(ClientSession clientSession) {
        return WsDebugSessionView.builder()
                .clientId(clientSession.getClientId())
                .open(clientSession.getSession().isOpen())
                .monitorSession(clientSession.isMonitorSession())
                .subscriptionTaskId(clientSession.getSubscriptionTaskId())
                .connectedAt(clientSession.getConnectedAt())
                .lastSeenAt(clientSession.getLastSeenAt())
                .remoteAddress(clientSession.getRemoteAddress())
                .userAgent(clientSession.getUserAgent())
                .build();
    }
}
