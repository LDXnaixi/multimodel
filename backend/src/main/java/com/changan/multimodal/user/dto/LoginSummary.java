package com.changan.multimodal.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class LoginSummary {

    private final int totalCount;
    private final int uniqueUsers;
    private final Map<String, Long> moduleAccessCount;
}
