package com.changan.multimodal.common.api;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final long serverTime;
    private final String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("0")
                .message("OK")
                .data(data)
                .serverTime(Instant.now().toEpochMilli())
                .traceId(TraceIdHolder.getTraceId())
                .build();
    }

    public static <T> ApiResponse<T> failure(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .serverTime(Instant.now().toEpochMilli())
                .traceId(TraceIdHolder.getTraceId())
                .build();
    }
}
