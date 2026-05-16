package com.changan.multimodal.user.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginStatRecord {

    private final String username;
    private final String ip;
    private final String module;
    private final String action;
    private final long loginTime;
}
