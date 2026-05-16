package com.changan.multimodal.realtime.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WsDebugSendRequest {

    private String clientId;

    @NotBlank(message = "type不能为空")
    private String type;

    private String requestId;

    private JsonNode payload;

    private boolean broadcast;
}
