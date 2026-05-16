# WebSocket 旁路监控测试页说明

本文档说明如何使用项目新增的 WebSocket 旁路监控测试页，对“独立前端 ↔ 当前后端”的消息收发过程进行观察与联调。

页面文件位于 [`ws-monitor.html`](../src/main/resources/static/ws-monitor.html)。

## 1. 适用场景

该页面主要解决下面这类问题：

- 有一个独立前端正在连接你的后端 WebSocket
- 你希望看到它是否真的连上了
- 你希望看到它发给后端的消息内容
- 你希望确认后端是否给它回了消息
- 你希望以“服务器主动下发消息”的方式测试该前端是否能正确接收

也就是说，这个页面不是普通业务联调页，而是一个“服务端旁路观测 + 主动发消息”的调试控制台。

## 2. 访问方式

启动应用后，浏览器访问：

- `http://localhost:8080/ws-monitor.html`

## 3. 页面能力

该页面提供三类核心能力。

### 3.1 监控在线 WebSocket 客户端

页面可通过 [`GET /api/v1/ws-debug/sessions`](../src/main/java/com/changan/multimodal/realtime/controller/WsDebugController.java:29) 拉取当前在线客户端列表，展示：

- `clientId`
- 是否为监控客户端
- 连接状态
- 订阅任务 ID
- 远端地址
- `User-Agent`
- 连接时间与最近活跃时间

这些信息来自 [`ClientSession`](../src/main/java/com/changan/multimodal/realtime/session/ClientSession.java:10) 与 [`WsDebugSessionView`](../src/main/java/com/changan/multimodal/realtime/dto/WsDebugSessionView.java:7)。

### 3.2 旁路监听第三方前端与服务端的消息

页面通过 WebSocket 发送 [`CLIENT_MONITOR_SUBSCRIBE`](../src/main/java/com/changan/multimodal/realtime/dto/WsMessageType.java:5) 将自己注册成监控会话。注册后，后端会把以下事件以 [`MONITOR_EVENT`](../src/main/java/com/changan/multimodal/realtime/dto/WsMessageType.java:9) 的形式推送给监控页：

- 客户端连接
- 客户端断开
- 客户端发给后端的消息
- 后端发给客户端的消息

对应逻辑位于 [`publishMonitorEvent()`](../src/main/java/com/changan/multimodal/realtime/service/WsMessageRouter.java:127)。

### 3.3 以服务端身份主动向客户端发消息

页面可通过 [`POST /api/v1/ws-debug/send`](../src/main/java/com/changan/multimodal/realtime/controller/WsDebugController.java:35) 触发后端向：

- 指定客户端发送消息
- 所有业务客户端广播消息

底层调用：

- [`pushToClient()`](../src/main/java/com/changan/multimodal/realtime/service/WsMessageRouter.java:90)
- [`pushToAllBusinessClients()`](../src/main/java/com/changan/multimodal/realtime/service/WsMessageRouter.java:101)

## 4. 对第三方前端的测试价值

借助该页面，你可以完成以下验证：

1. 第三方前端是否真的连上你的后端。
2. 第三方前端给后端发的消息，后端是否实际收到了。
3. 后端是否已经向第三方前端回包。
4. 你手工从服务端发出的消息，第三方前端是否能正确接收与处理。

## 5. 当前协议行为补充

当前 [`handleMessage()`](../src/main/java/com/changan/multimodal/realtime/service/WsMessageRouter.java:40) 除支持：

- [`CLIENT_SUBSCRIBE`](../src/main/java/com/changan/multimodal/realtime/dto/WsMessageType.java:5)
- [`CLIENT_MONITOR_SUBSCRIBE`](../src/main/java/com/changan/multimodal/realtime/dto/WsMessageType.java:6)
- [`CLIENT_PONG`](../src/main/java/com/changan/multimodal/realtime/dto/WsMessageType.java:7)

之外，对于未显式处理的自定义客户端消息，也会返回一个 `SERVER_ACK`，状态为 `RECEIVED_UNHANDLED`。这意味着：

- 独立前端即使发送自定义 `type`
- 后端也能记录并回一个基础确认包
- 你可以先验证“链路是否通”，再决定是否补充真正的业务处理逻辑

## 6. 推荐使用步骤

1. 打开 [`ws-monitor.html`](../src/main/resources/static/ws-monitor.html)。
2. 点击“连接并订阅监控”。
3. 让独立前端连接你的后端 WebSocket。
4. 在“在线客户端”区域确认其已出现。
5. 让独立前端发送消息，观察监控日志中的 `CLIENT_MESSAGE`。
6. 观察后端返回给它的 `SERVER_MESSAGE` 是否出现。
7. 在页面中选择该客户端，点击“发给目标客户端”主动下发一条消息。
8. 让对方确认其前端是否成功接收并处理。

## 7. 与原联调页的区别

- [`comm-test.html`](../src/main/resources/static/comm-test.html) 更偏向“本页面自己作为客户端去联调后端”。
- [`ws-monitor.html`](../src/main/resources/static/ws-monitor.html) 更偏向“观察别的客户端与后端的实时收发行为，并从后端主动下发消息”。

两者可以同时打开，分别承担不同联调角色。
