package com.changan.multimodal.common.api;

public final class TraceIdHolder {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceIdHolder() {
    }

    public static void setTraceId(String traceId) {
        HOLDER.set(traceId);
    }

    public static String getTraceId() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
