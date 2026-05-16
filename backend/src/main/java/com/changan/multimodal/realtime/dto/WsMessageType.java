package com.changan.multimodal.realtime.dto;

public final class WsMessageType {

    public static final String CLIENT_SUBSCRIBE = "CLIENT_SUBSCRIBE";
    public static final String CLIENT_MONITOR_SUBSCRIBE = "CLIENT_MONITOR_SUBSCRIBE";
    public static final String CLIENT_PONG = "CLIENT_PONG";
    public static final String SERVER_ACK = "SERVER_ACK";
    public static final String SERVER_PING = "SERVER_PING";
    public static final String MONITOR_EVENT = "MONITOR_EVENT";
    public static final String TASK_PROGRESS = "TASK_PROGRESS";
    public static final String TASK_STATUS = "TASK_STATUS";
    public static final String ALERT = "ALERT";
    public static final String RESOURCE_METRIC = "RESOURCE_METRIC";
    public static final String RESOURCE_ALERT = "RESOURCE_ALERT";
    public static final String MODEL_RESULT = "MODEL_RESULT";
    public static final String DATA_PIPELINE = "DATA_PIPELINE";

    private WsMessageType() {
    }
}
