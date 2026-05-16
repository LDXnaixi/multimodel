# 内置联调测试页说明文档

本文档说明项目内置联调测试页的用途、访问方式、页面功能、典型联调流程以及常见问题，便于前后端、接口联调、WebSocket 推送验证和接口排障时直接使用。

## 1. 页面定位

内置联调测试页是由后端静态资源直接托管的一个轻量级调试页面，用于替代临时脚本、Postman + WebSocket 插件、独立前端调试页等零散联调方式，帮助开发人员在浏览器中直接完成以下动作：

- 连接后端 WebSocket 服务
- 发起 HTTP 接口调用
- 观察实时推送消息
- 自动响应服务端心跳
- 快速执行任务、推理、数据流水线、报告导出等联调动作
- 手工构造原始 HTTP / WebSocket 消息进行排障

页面实现位于 [`comm-test.html`](../src/main/resources/static/comm-test.html)。

## 2. 访问方式

在应用启动后，直接通过浏览器访问：

- `http://localhost:8080/comm-test.html`

该页面由 Spring Boot 静态资源目录直接提供，无需额外前端构建步骤。

## 3. 页面覆盖的联调范围

内置联调测试页主要覆盖两类通讯能力：

### 3.1 REST 接口联调

页面内置了一组快捷操作按钮，可快速验证以下接口能力：

- 系统健康检查
- 模型列表查询
- 任务创建、启动、控制
- 推理执行
- 数据集注册
- 数据流水线执行
- 报告查询与导出
- 用户模拟登录与登录统计
- 资源监控与告警查询

### 3.2 WebSocket 联调

页面可以直接连接后端进度推送通道，并支持：

- 建立与断开 WebSocket 连接
- 接收服务端推送消息
- 自动处理 `SERVER_PING` 并回复 `CLIENT_PONG`
- 手工发送自定义 WebSocket JSON 消息
- 查看完整通讯日志

## 4. 页面结构说明

页面可以分为四个功能区。

## 4.1 连接配置区

该区域用于配置基础连接信息，包括：

- `HTTP Base URL`：HTTP 请求基础地址
- `WebSocket URL`：WebSocket 推送地址
- `X-User-Name`：请求头中的用户名标识
- WebSocket 状态展示
- 连接、断开、清空日志按钮

页面初始化时，会根据浏览器当前访问地址自动推断默认值：

- `HTTP Base URL` 默认取当前页面来源地址
- `WebSocket URL` 自动从 `http/https` 推导为 `ws/wss`

相关初始化逻辑位于 [`init()`](../src/main/resources/static/comm-test.html:277)。

## 4.2 快捷联调区

该区域封装了一组常用联调动作按钮，便于按业务流快速触发后端接口。页面已集成：

- 健康检查
- 查询模型
- 资源快照
- 告警列表
- 模拟登录
- 创建任务
- 订阅任务
- 启动任务
- 控制任务
- 模型推理
- 注册数据集
- 运行流水线
- 查询报告
- 导出 JSON / CSV / XML

相关按钮绑定逻辑位于 [`bindEvents()`](../src/main/resources/static/comm-test.html:288)。

## 4.3 HTTP 请求体编辑区

该区域用于手工调试任意 HTTP 接口，支持：

- 切换请求方法
- 自定义请求路径
- 自定义 JSON 请求体
- 发送请求并在日志区查看响应

适合以下场景：

- 后端新增接口后快速验证
- 前端联调时复现某个特定请求
- 构造异常请求验证错误处理逻辑

发送逻辑位于 [`sendCustomHttp()`](../src/main/resources/static/comm-test.html:628)，底层统一请求封装位于 [`request()`](../src/main/resources/static/comm-test.html:332)。

## 4.4 WebSocket 发消息区

该区域用于手工构造原始 WebSocket 消息，支持：

- 输入任意 JSON 消息体
- 发送自定义 WebSocket 消息
- 手工发送 `CLIENT_PONG`

适合以下场景：

- 验证订阅协议格式
- 模拟前端异常消息
- 对 WebSocket 接口做协议级排查

发送逻辑位于 [`sendCustomWs()`](../src/main/resources/static/comm-test.html:640) 与 [`sendWs()`](../src/main/resources/static/comm-test.html:412)。

## 4.5 通讯日志区

页面底部提供统一日志窗口，记录以下信息：

- 系统初始化信息
- HTTP 请求与响应
- WebSocket 发送与接收消息
- 连接状态变化
- 错误信息

日志输出逻辑位于 [`log()`](../src/main/resources/static/comm-test.html:317)。

## 5. 页面默认行为说明

为了降低联调门槛，页面内置了一些默认行为。

### 5.1 自动推断连接地址

页面打开后，会根据当前浏览器地址自动填充默认 HTTP 和 WebSocket 地址，减少手工输入。

### 5.2 自动响应心跳

当收到服务端 `SERVER_PING` 消息时，页面会自动回发 `CLIENT_PONG`，避免连接因未响应心跳而被服务端回收。

对应逻辑位于 [`state.socket.onmessage`](../src/main/resources/static/comm-test.html:376)。

### 5.3 自动回填任务 ID 与数据集 ID

页面执行“创建任务”后，会自动把响应中的 `taskId` 回填到输入框，并同步刷新默认订阅消息体；执行“注册数据集”后，会自动回填 `datasetId`。

对应逻辑分别位于：

- [`createTask()`](../src/main/resources/static/comm-test.html:429)
- [`registerDataset()`](../src/main/resources/static/comm-test.html:532)

### 5.4 自动附带追踪头

所有通过统一请求封装发送的 HTTP 请求，默认都会附带：

- `Content-Type: application/json`
- `X-Trace-Id`
- `X-User-Name`

便于后端链路追踪和审计联调。

## 6. 快捷联调能力说明

下面对页面内置的主要快捷动作做说明。

### 6.1 健康检查

调用系统健康检查接口，快速确认服务是否在线。

- 接口：`GET /api/v1/system/health`

### 6.2 查询模型

查询当前后端暴露的模型列表。

- 接口：`GET /api/v1/models`

### 6.3 创建任务

创建一个默认的多模态联调任务，请求体中已预置“数据接入 / 模型调用 / 报告导出”三个节点，适合作为联调样例。

- 接口：`POST /api/v1/tasks`

成功后页面会：

- 回填任务 ID
- 生成默认的任务订阅 WebSocket 消息

### 6.4 订阅任务

通过 WebSocket 发送任务订阅消息，接收指定任务的实时进度推送。

### 6.5 启动任务

调用启动接口后，若已完成 WebSocket 订阅，日志区可持续观察任务进度推送。

- 接口：`POST /api/v1/tasks/{taskId}/start`

### 6.6 控制任务

支持以下控制动作：

- `PAUSE`
- `RESUME`
- `RERUN`
- `TERMINATE`

调用接口：`POST /api/v1/tasks/{taskId}/control`

### 6.7 模型推理

发送预置推理请求，用于验证推理接口、结果返回和实时广播。

- 接口：`POST /api/v1/inference/run`

### 6.8 注册数据集

向后端注册联调用测试数据集，并自动回填 `datasetId`。

- 接口：`POST /api/v1/data/datasets/register`

### 6.9 运行流水线

基于当前数据集 ID 触发一次数据流水线执行，便于测试数据处理协议。

- 接口：`POST /api/v1/data/pipelines/run`

### 6.10 查询报告与导出

支持查询报告详情与导出三种格式文件：

- `GET /api/v1/reports/{taskId}`
- `GET /api/v1/reports/{taskId}/export?format=JSON`
- `GET /api/v1/reports/{taskId}/export?format=CSV`
- `GET /api/v1/reports/{taskId}/export?format=XML`

导出逻辑位于 [`exportReport()`](../src/main/resources/static/comm-test.html:586)。

### 6.11 模拟登录

通过模拟登录接口记录用户访问，并联动查询登录统计信息。

- `POST /api/v1/users/mock-login`
- `GET /api/v1/users/login-summary`

## 7. 推荐联调流程

建议按照以下顺序使用内置联调测试页。

### 7.1 验证服务是否启动

先访问页面并点击“健康检查”，确认 HTTP 服务可用。

### 7.2 建立 WebSocket 连接

点击“连接 WS”，确认状态显示为“已连接”，并观察日志区是否打印连接成功。

### 7.3 创建并订阅任务

点击“创建任务”后，再点击“订阅任务”，确保后续任务启动时能收到实时推送。

### 7.4 启动任务并观察推送

点击“启动任务”，观察日志区是否收到 `TASK_PROGRESS` 等实时消息。

### 7.5 验证扩展能力

继续测试以下功能：

- 模型推理
- 数据集注册
- 数据流水线执行
- 资源快照与告警查询
- 报告查询与导出
- 用户登录统计

### 7.6 做协议级排障

若某个接口联调异常，可切换到：

- HTTP 请求体编辑区：复现任意 REST 请求
- WebSocket 发消息区：复现任意原始 WS 消息

## 8. 适用场景

内置联调测试页适合以下场景：

- 前后端初次联调
- WebSocket 推送协议验证
- 新接口开发后的自测
- 无前端页面时的后端独立验证
- Bug 复现与排障
- 演示环境下的快速接口验收

## 9. 常见问题

### 9.1 页面可以打开，但接口请求失败

优先检查：

- 后端服务是否已启动
- `HTTP Base URL` 是否正确
- 接口路径是否以 `/api/v1` 开头
- 请求体 JSON 是否合法

### 9.2 WebSocket 无法连接

优先检查：

- `WebSocket URL` 是否正确
- 当前环境是否存在代理或网关改写
- 后端 WebSocket 路径是否为 `/ws/progress`

### 9.3 WebSocket 连接会自动断开

通常需要确认：

- 是否成功接收到 `SERVER_PING`
- 页面是否已自动回复 `CLIENT_PONG`
- 服务端是否因长时间无响应关闭连接

### 9.4 启动任务后没有实时进度

通常是以下原因之一：

- 尚未建立 WebSocket 连接
- 尚未发送任务订阅消息
- 任务 ID 无效
- 任务并未真正启动成功

### 9.5 导出文件没有下载

请确认：

- 当前 `taskId` 存在
- 报告接口正常返回二进制流
- 浏览器未拦截下载动作

## 10. 维护建议

后续若后端增加新接口或新增新的 WebSocket 消息类型，建议同步维护以下内容：

1. 在 [`comm-test.html`](../src/main/resources/static/comm-test.html) 中补充快捷按钮或示例消息。
2. 在本文档中补充新能力说明和使用方法。
3. 如协议有变更，同步更新 [`FRONTEND_PROTOCOL_GUIDE.md`](./FRONTEND_PROTOCOL_GUIDE.md)。

## 11. 总结

内置联调测试页的核心价值不是替代正式前端，而是为后端、前端、测试提供一个统一、轻量、零构建成本的联调入口。通过该页面可以快速完成 HTTP + WebSocket 的协议验证、业务流程联调和问题复现，提高接口开发和排障效率。
