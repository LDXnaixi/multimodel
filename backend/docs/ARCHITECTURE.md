# 后端总体架构设计

## 1. 设计依据

- 以 [`协议.txt`](../协议.txt) 中“B/S 架构、后端微服务、模型服务化封装、任务状态实时反馈、资源监控、日志追溯”的要求为主。
- 以 [`需求清单解析.txt`](../需求清单解析.txt) 中 ARC、TSK、ENV、RPT 条目作为接口和事件抽象边界。

## 2. 本周交付目标

本次交付优先完成“前后端通讯联调骨架”，其它复杂业务模块先保留 TODO 占位，但通讯协议、推送机制、并发执行与心跳保活机制已经预留清晰扩展点。

## 3. 推荐微服务划分

1. **gateway-service**：统一入口、鉴权、限流、TraceId 透传。
2. **task-service**：测试任务、流程编排、节点状态机、控制动作。
3. **model-service**：模型注册、模型路由、版本切换、部署状态管理。
4. **data-service**：多模态数据接入、元数据、版本、数据增强流水线。
5. **monitor-service**：资源监控、运行指标、告警、环境要素采集。
6. **report-service**：报告汇总、日志追溯、导出。
7. **ws-push-service**：前端实时订阅、进度推送、心跳保活。

当前代码为便于本周联调，先将上述能力收敛在单体工程中，后续可按包边界拆分为微服务。

## 4. 通讯设计

### 4.1 同步通讯

- 前端 → 后端：HTTP/JSON REST。
- 用途：列表、详情、创建任务、启动/暂停/继续/终止、模型查询、健康检查。
- 返回统一信封：`success/code/message/data/serverTime/traceId`。

### 4.2 异步通讯

- 前端 ↔ 后端：WebSocket。
- 用途：任务进度、节点状态变化、资源告警、系统心跳。
- 优势：比轮询更适合 [`协议.txt`](../协议.txt:46) 与 [`协议.txt`](../协议.txt:51) 要求的实时状态反馈。

### 4.3 心跳机制

- 服务端每 10 秒发送一次 `SERVER_PING`。
- 前端收到后立即回复 `CLIENT_PONG`。
- 若 30 秒内未收到 pong，则判定客户端订阅失效并回收连接。
- 该机制可用于页面刷新、网络抖动、浏览器标签页挂起时的连接清理。

## 5. 并发模型

- 基于 [`AsyncConfig`](../src/main/java/com/changan/multimodal/common/config/AsyncConfig.java) 中线程池执行任务模拟流程。
- 当前线程池参数：核心 8、最大 32、队列 500。
- 拒绝策略：`CallerRunsPolicy`，确保高峰期不直接丢任务。
- 后续建议：
  - 任务级并发：按任务优先级分池。
  - 节点级并发：无依赖节点并行执行。
  - 资源级并发：结合 CPU/GPU/显存配额限流。
  - 模型调用级并发：采用熔断、隔离仓、超时控制。

## 6. 状态机设计

### 6.1 任务状态

- `CREATED`
- `RUNNING`
- `PAUSED`
- `SUCCESS`
- `FAILED`
- `TERMINATED`

### 6.2 节点状态

- `PENDING`
- `RUNNING`
- `SUCCESS`
- `FAILED`
- `TIMEOUT`
- `WAITING_RESOURCE`
- `SKIPPED`

这与 [`协议.txt`](../协议.txt:44) 的状态捕捉要求对齐。

## 7. 目录职责

- [`src/main/java/com/changan/multimodal/task`](../src/main/java/com/changan/multimodal/task) ：任务接口、状态机、流程模拟。
- [`src/main/java/com/changan/multimodal/realtime`](../src/main/java/com/changan/multimodal/realtime) ：WebSocket 消息、会话、心跳。
- [`src/main/java/com/changan/multimodal/model`](../src/main/java/com/changan/multimodal/model) ：模型注册查询占位。
- [`src/main/java/com/changan/multimodal/health`](../src/main/java/com/changan/multimodal/health) ：系统健康检查。
- [`src/main/java/com/changan/multimodal/common`](../src/main/java/com/changan/multimodal/common) ：统一响应、异常、TraceId、线程池配置。

## 8. 后续扩展建议

1. 将任务数据、模型元数据、运行日志落库。
2. 将 WebSocket 改为 STOMP 或接入消息总线实现跨实例推送。
3. 引入 Redis 维护分布式会话与幂等控制。
4. 引入 MQ/Kafka 承载任务事件流。
5. 引入 Prometheus + Grafana 实现资源监控与指标可视化。
6. 将当前已补充的 [`InferenceController`](../src/main/java/com/changan/multimodal/inference/controller/InferenceController.java)、[`DataPipelineController`](../src/main/java/com/changan/multimodal/data/controller/DataPipelineController.java)、[`MonitorController`](../src/main/java/com/changan/multimodal/monitor/controller/MonitorController.java)、[`ReportController`](../src/main/java/com/changan/multimodal/report/controller/ReportController.java)、[`UserAuditController`](../src/main/java/com/changan/multimodal/user/controller/UserAuditController.java) 逐步替换为数据库与真实模型依赖版本。
