package com.changan.multimodal.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class LoginStatRecord {

    private final String username;
    private final String ip;
    private final String module;
    private final String action;
    private final long loginTime;
}
