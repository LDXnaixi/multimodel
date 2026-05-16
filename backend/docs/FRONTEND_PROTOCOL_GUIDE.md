# 前端通讯协议接入指南

本文档面向前端开发人员，说明如何与当前后端骨架完成联调。

## 0. 内置联调测试页

后端已经内置一个可直接打开的通讯测试页：

- 访问地址：`http://localhost:8080/comm-test.html`

用途：

- 直接连接 WebSocket 并实时显示服务端推送
- 自动响应 `SERVER_PING`
- 快速发起任务创建、任务订阅、任务启动、模型推理、数据流水线、资源监控、报告导出、用户登录统计请求
- 支持手工发送原始 HTTP/WS 消息，便于前后端联调排障

## 1. 基础约定

- HTTP Base URL：`http://localhost:8080`
- WebSocket URL：`ws://localhost:8080/ws/progress`
- Header：建议所有请求透传 `X-Trace-Id`
- 数据格式：`application/json`
- 时间戳：毫秒级 Unix 时间戳

## 2. REST 响应格式

```json
{
  "success": true,
  "code": "0",
  "message": "OK",
  "data": {},
  "serverTime": 1710000000000,
  "traceId": "1d9d9ab4ddbc4a6a8c50d0c7eb2e4f5f"
}
```

## 3. HTTP 接口

### 3.1 系统健康检查

- `GET /api/v1/system/health`

响应字段：

- `service`：服务名
- `status`：服务状态
- `websocketClients`：当前在线 websocket 客户端数量

### 3.2 查询模型列表

- `GET /api/v1/models`

说明：当前仅返回占位模型，用于联调前端页面。

### 3.3 创建任务

- `POST /api/v1/tasks`

请求示例：

```json
{
  "taskName": "多模态联调测试任务",
  "scenarioDescription": "验证前后端通讯、心跳与进度推送",
  "nodes": [
    {
      "nodeId": "data-access",
      "nodeName": "数据接入",
      "nodeType": "DATA",
      "priority": 1,
      "resourceRatio": 0.2,
      "parameters": {
        "sourceType": "image"
      }
    },
    {
      "nodeId": "model-invoke",
      "nodeName": "模型调用",
      "nodeType": "MODEL",
      "priority": 2,
      "resourceRatio": 0.5,
      "parameters": {
        "modelId": "yolov8-demo"
      }
    }
  ]
}
```

### 3.4 查询任务列表

- `GET /api/v1/tasks`

### 3.5 查询任务详情

- `GET /api/v1/tasks/{taskId}`

### 3.6 启动任务

- `POST /api/v1/tasks/{taskId}/start`

说明：启动后，后端会通过 WebSocket 按节点推送进度。

### 3.7 控制任务

- `POST /api/v1/tasks/{taskId}/control`

请求体：

```json
{
  "action": "PAUSE"
}
```

可选值：

- `PAUSE`
- `RESUME`
- `RERUN`
- `TERMINATE`

### 3.8 真实模型推理与评测

- `POST /api/v1/inference/run`

请求示例：

```json
{
  "modelId": "yolov8-demo",
  "modality": "image",
  "inputs": [
    {
      "inputId": "img-001",
      "sourceUri": "samples/car.png",
      "attributes": {
        "scene": "factory"
      }
    }
  ],
  "requestedMetrics": ["mAP", "Precision", "Recall"],
  "options": {
    "batchSize": 1
  }
}
```

用途：前端发起统一推理请求，后端返回推理结果和评测指标，并同时向 WebSocket 广播 `MODEL_RESULT`。

### 3.9 多模态数据集注册

- `POST /api/v1/data/datasets/register`

请求示例：

```json
{
  "datasetName": "联调数据集",
  "assets": [
    {
      "assetId": "asset-001",
      "uri": "samples/a.wav",
      "modality": "audio",
      "tags": ["speech", "test"]
    }
  ],
  "filterRules": {
    "speaker": "demo"
  }
}
```

### 3.10 数据流水线执行

- `POST /api/v1/data/pipelines/run`

请求示例：

```json
{
  "datasetId": "上一步返回的数据集ID",
  "operations": [
    {
      "operation": "normalize",
      "parameters": {
        "scale": "0-1"
      }
    },
    {
      "operation": "augment.cutmix",
      "parameters": {
        "ratio": 0.3
      }
    }
  ]
}
```

用途：前端配置数据清洗、归一化、增强流程；后端执行后返回产物列表，并广播 `DATA_PIPELINE`。

### 3.11 资源监控与阈值

- `GET /api/v1/monitor/metrics`
- `GET /api/v1/monitor/alerts`
- `POST /api/v1/monitor/thresholds`

阈值更新示例：

```json
{
  "cpuUsageThreshold": 75,
  "memoryUsageThreshold": 80
}
```

用途：前端轮询或订阅资源状态，同时监听 `RESOURCE_METRIC` 与 `RESOURCE_ALERT` 推送。

### 3.12 报告查询与导出

- `GET /api/v1/reports/{taskId}`
- `GET /api/v1/reports/{taskId}/export?format=CSV`
- `GET /api/v1/reports/{taskId}/export?format=JSON`
- `GET /api/v1/reports/{taskId}/export?format=XML`

用途：报告页直接展示聚合结果，并下载导出文件。

### 3.13 用户登录统计

- `POST /api/v1/users/mock-login`
- `GET /api/v1/users/login-stats`
- `GET /api/v1/users/login-summary`

登录示例：

```json
{
  "username": "frontend-dev",
  "module": "dashboard",
  "ip": "127.0.0.1"
}
```

说明：除显式登录记录外，后端还会自动记录接口访问审计。前端可透传请求头 `X-User-Name` 以便统计用户调用模块信息。

## 4. WebSocket 协议

### 4.1 连接后第一步：订阅任务

前端连接 [`/ws/progress`](../src/main/java/com/changan/multimodal/common/config/WebSocketConfig.java) 后发送：

```json
{
  "type": "CLIENT_SUBSCRIBE",
  "requestId": "req-001",
  "payload": {
    "taskId": "任务ID"
  }
}
```

后端响应：

```json
{
  "type": "SERVER_ACK",
  "timestamp": 1710000000000,
  "requestId": "req-001",
  "payload": {
    "status": "SUBSCRIBED",
    "taskId": "任务ID"
  }
}
```

### 4.2 心跳机制

后端定时发送：

```json
{
  "type": "SERVER_PING",
  "timestamp": 1710000000000,
  "requestId": "ping-1710000000000",
  "payload": null
}
```

前端必须立即回复：

```json
{
  "type": "CLIENT_PONG",
  "requestId": "ping-1710000000000",
  "payload": {}
}
```

若 30 秒内未回 pong，后端将回收连接。

### 4.3 任务进度推送

后端示例消息：

```json
{
  "type": "TASK_PROGRESS",
  "timestamp": 1710000000100,
  "requestId": "任务ID",
  "payload": {
    "taskId": "任务ID",
    "taskStatus": "RUNNING",
    "nodeId": "model-invoke",
    "nodeStatus": "RUNNING",
    "progress": 50,
    "message": "节点开始执行，当前为通讯联调模拟流程"
  }
}
```

### 4.4 模型结果推送

```json
{
  "type": "MODEL_RESULT",
  "timestamp": 1710000000200,
  "requestId": "MODEL_RESULT-1710000000200",
  "payload": {
    "jobId": "推理任务ID",
    "modelId": "yolov8-demo",
    "modality": "image",
    "finishedAt": 1710000000200,
    "startedAt": 1710000000000,
    "status": "COMPLETED"
  }
}
```

### 4.5 数据流水线推送

```json
{
  "type": "DATA_PIPELINE",
  "timestamp": 1710000000300,
  "requestId": "DATA_PIPELINE-1710000000300",
  "payload": {
    "pipelineId": "流水线ID",
    "datasetId": "数据集ID",
    "datasetName": "联调数据集",
    "operations": ["normalize", "augment.cutmix"],
    "status": "COMPLETED"
  }
}
```

### 4.6 资源指标推送

```json
{
  "type": "RESOURCE_METRIC",
  "timestamp": 1710000000400,
  "requestId": "RESOURCE_METRIC-1710000000400",
  "payload": {
    "cpuUsage": 23.8,
    "memoryUsage": 57.3,
    "heapUsage": 31.5,
    "diskUsage": 64.8,
    "networkThroughput": 84.2,
    "gpuUsage": 27.0,
    "cpuUsageThreshold": 75,
    "memoryUsageThreshold": 80,
    "timestamp": 1710000000400
  }
}
```

### 4.7 告警推送

```json
{
  "type": "RESOURCE_ALERT",
  "timestamp": 1710000000500,
  "requestId": "RESOURCE_ALERT-1710000000500",
  "payload": {
    "alertId": "告警ID",
    "level": "WARN",
    "category": "CPU",
    "message": "CPU占用超过阈值: 92.5%",
    "timestamp": 1710000000500
  }
}
```

## 5. 前端建议实现

### 5.1 WebSocket 封装要点

1. 建立连接后缓存 `socket` 实例。
2. 连接成功后立即发送 `CLIENT_SUBSCRIBE`。
3. 收到 `SERVER_PING` 立即回复 `CLIENT_PONG`。
4. 收到 `TASK_PROGRESS` 后更新任务进度条和节点状态。
5. 连接关闭时执行指数退避重连。

### 5.2 React/Vue 通用伪代码

```javascript
const socket = new WebSocket('ws://localhost:8080/ws/progress');

socket.onopen = () => {
  socket.send(JSON.stringify({
    type: 'CLIENT_SUBSCRIBE',
    requestId: `sub-${Date.now()}`,
    payload: { taskId }
  }));
};

socket.onmessage = (event) => {
  const message = JSON.parse(event.data);

  if (message.type === 'SERVER_PING') {
    socket.send(JSON.stringify({
      type: 'CLIENT_PONG',
      requestId: message.requestId,
      payload: {}
    }));
    return;
  }

  if (message.type === 'TASK_PROGRESS') {
    updateTaskStore(message.payload);
  }

  if (message.type === 'RESOURCE_METRIC') {
    updateMonitorPanel(message.payload);
  }

  if (message.type === 'RESOURCE_ALERT') {
    pushAlertToast(message.payload);
  }
};
```

## 6. 联调顺序建议

1. 先调通 [`GET /api/v1/system/health`](../src/main/java/com/changan/multimodal/health/controller/HealthController.java)。
2. 再调通 [`POST /api/v1/tasks`](../src/main/java/com/changan/multimodal/task/controller/TaskController.java) 创建任务。
3. 建立 WebSocket 并发送订阅。
4. 调用 [`POST /api/v1/tasks/{taskId}/start`](../src/main/java/com/changan/multimodal/task/controller/TaskController.java) 观察实时进度。
5. 联调 [`POST /api/v1/inference/run`](../src/main/java/com/changan/multimodal/inference/controller/InferenceController.java)、[`POST /api/v1/data/pipelines/run`](../src/main/java/com/changan/multimodal/data/controller/DataPipelineController.java) 与实时推送。
6. 联调 [`GET /api/v1/monitor/metrics`](../src/main/java/com/changan/multimodal/monitor/controller/MonitorController.java) 与资源告警。
7. 联调 [`GET /api/v1/reports/{taskId}/export`](../src/main/java/com/changan/multimodal/report/controller/ReportController.java) 报告导出。
8. 最后联调 [`POST /api/v1/tasks/{taskId}/control`](../src/main/java/com/changan/multimodal/task/controller/TaskController.java) 的暂停/继续/终止，以及 [`GET /api/v1/users/login-summary`](../src/main/java/com/changan/multimodal/user/controller/UserAuditController.java) 的访问统计。

## 7. 当前占位说明

以下能力已提供可联调接口，但内部实现仍以“统一协议 + 可运行模拟”方式交付，便于本周前后端对接：

- 真实模型推理与评测协议接口
- 多模态数据接入与增强流水线协议接口
- 资源监控与告警接口
- 报告查询与 CSV/JSON/XML 导出接口
- 用户登录统计与访问审计接口

它们与 [`协议.txt`](../协议.txt:47) 至 [`协议.txt`](../协议.txt:59) 对齐，后续只需将内部模拟实现替换为真实引擎，无需改变前后端接口协议。
