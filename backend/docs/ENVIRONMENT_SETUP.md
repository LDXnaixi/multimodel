# 环境配置说明

本文档用于记录当前项目的开发环境、运行环境、硬件约束和本地联调配置，便于后续开发、部署和验收时统一口径。

## 1. 项目基础信息

- 项目名称：多模态融合测试运行工具后端通讯服务
- 项目类型：Java Spring Boot 后端
- 构建方式：Maven
- 项目根目录：`c:/Users/182562/Desktop/test_tool`
- 主要启动类：[`MultimodalTestBackendApplication`](../src/main/java/com/changan/multimodal/MultimodalTestBackendApplication.java:10)
- Maven 配置文件：[`pom.xml`](../pom.xml)
- 应用配置文件：[`application.yml`](../src/main/resources/application.yml)

## 2. 当前开发机环境

根据当前实际终端环境，开发机配置如下：

### 2.1 操作系统

- 操作系统：Windows 11
- 默认终端：`C:\WINDOWS\system32\cmd.exe`
- 当前工作目录：`c:/Users/182562/Desktop/test_tool`

### 2.2 Java 环境

- Java 版本：19.0.2
- Java 发行商：Oracle Corporation
- Java 运行时目录：`C:\Program Files\Java\jdk-19`

说明：

- 当前机器安装的是 JDK 19。
- 但项目在 [`pom.xml`](../pom.xml) 中配置的编译目标版本为 `Java 17`，因此只要本机 JDK 版本 **大于等于 17** 即可正常编译运行。

### 2.3 Maven 环境

- Maven 版本：3.9.9
- Maven 安装目录：`D:\maven3.9\apache-maven-3.9.9`

## 3. 项目编译配置

项目使用 Spring Boot + Maven，核心配置见 [`pom.xml`](../pom.xml)。

### 3.1 当前关键依赖

- `spring-boot-starter-web`
- `spring-boot-starter-websocket`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `jackson-dataformat-xml`
- `lombok`
- `spring-boot-starter-test`

### 3.2 Java 编译版本

[`pom.xml`](../pom.xml) 中当前配置：

- `java.version = 17`

这意味着：

- 推荐部署环境最低 JDK 17
- 当前本地使用 JDK 19 编译和测试已通过

## 4. 本地运行配置

### 4.1 服务端口

[`application.yml`](../src/main/resources/application.yml) 当前配置：

- 服务端口：`8080`
- Spring 应用名：`multimodal-test-backend`

### 4.2 本地启动命令

在项目根目录执行：

```bash
mvn spring-boot:run
```

或先打包再运行：

```bash
mvn clean package
java -jar target/multimodal-test-backend-0.0.1-SNAPSHOT.jar
```

### 4.3 本地验证命令

当前项目已经通过以下命令验证：

```bash
mvn test
```

## 5. 本地联调访问地址

服务启动后，可使用以下地址进行联调：

### 5.1 HTTP 基础地址

- `http://localhost:8080`

### 5.2 WebSocket 地址

- `ws://localhost:8080/ws/progress`

### 5.3 内置通讯测试页

- `http://localhost:8080/comm-test.html`

测试页文件位置：[`comm-test.html`](../src/main/resources/static/comm-test.html)

### 5.4 常用接口

- 健康检查：`GET /api/v1/system/health`
- 模型列表：`GET /api/v1/models`
- 创建任务：`POST /api/v1/tasks`
- 启动任务：`POST /api/v1/tasks/{taskId}/start`
- 模型推理：`POST /api/v1/inference/run`
- 数据集注册：`POST /api/v1/data/datasets/register`
- 数据流水线：`POST /api/v1/data/pipelines/run`
- 资源监控：`GET /api/v1/monitor/metrics`
- 报告查询：`GET /api/v1/reports/{taskId}`
- 用户登录统计：`GET /api/v1/users/login-summary`

前端联调协议说明见：[`FRONTEND_PROTOCOL_GUIDE.md`](./FRONTEND_PROTOCOL_GUIDE.md)

## 6. 当前后端功能模块对应环境入口

### 6.1 实时通讯模块

- WebSocket 配置：[`WebSocketConfig`](../src/main/java/com/changan/multimodal/common/config/WebSocketConfig.java:11)
- WebSocket 处理器：[`ProgressWebSocketHandler`](../src/main/java/com/changan/multimodal/realtime/ws/ProgressWebSocketHandler.java:12)
- 心跳服务：[`HeartbeatService`](../src/main/java/com/changan/multimodal/realtime/service/HeartbeatService.java:17)

### 6.2 任务与流程模块

- 任务接口：[`TaskController`](../src/main/java/com/changan/multimodal/task/controller/TaskController.java:18)
- 任务服务：[`TaskService`](../src/main/java/com/changan/multimodal/task/service/TaskService.java:35)

### 6.3 模型推理模块

- 推理接口：[`InferenceController`](../src/main/java/com/changan/multimodal/inference/controller/InferenceController.java:14)
- 推理服务：[`ModelInferenceService`](../src/main/java/com/changan/multimodal/inference/service/ModelInferenceService.java:20)

### 6.4 数据流水线模块

- 数据接口：[`DataPipelineController`](../src/main/java/com/changan/multimodal/data/controller/DataPipelineController.java:16)
- 数据服务：[`DataPipelineService`](../src/main/java/com/changan/multimodal/data/service/DataPipelineService.java:19)

### 6.5 资源监控模块

- 监控接口：[`MonitorController`](../src/main/java/com/changan/multimodal/monitor/controller/MonitorController.java:17)
- 监控服务：[`ResourceMonitorService`](../src/main/java/com/changan/multimodal/monitor/service/ResourceMonitorService.java:18)

### 6.6 报告导出模块

- 报告接口：[`ReportController`](../src/main/java/com/changan/multimodal/report/controller/ReportController.java:16)
- 报告服务：[`ReportService`](../src/main/java/com/changan/multimodal/report/service/ReportService.java:21)

### 6.7 用户审计模块

- 用户接口：[`UserAuditController`](../src/main/java/com/changan/multimodal/user/controller/UserAuditController.java:17)
- 审计过滤器：[`AccessAuditFilter`](../src/main/java/com/changan/multimodal/user/filter/AccessAuditFilter.java:14)
- 审计服务：[`UserAuditService`](../src/main/java/com/changan/multimodal/user/service/UserAuditService.java:15)

## 7. 协议约束下的目标部署环境

根据 [`协议.txt`](../协议.txt:65) 至 [`协议.txt`](../协议.txt:76) 以及 [`需求清单解析.txt`](../需求清单解析.txt:75)，目标硬件环境约束如下：

- CPU：2 颗鲲鹏 920，高性能多核服务器级处理器
- 核心数：总核心数不低于 64 核
- 主频：2.6GHz 及以上
- 内存：256GB
- 存储：4TB SSD
- 显卡：海光 K100，显存 64G × 4
- AI 加速能力：FP16 不低于 300 TFLOPS
- 网络：1000M 网络接口至少 1 路

说明：

- 当前本地开发环境是 Windows 11 + JDK 19 + Maven 3.9.9，用于开发联调。
- 最终交付环境应以 [`协议.txt`](../协议.txt:21) 中的 B/S 架构、微服务部署和硬件指标为准。

## 8. 推荐部署环境要求

为了和当前项目保持一致，建议部署环境至少满足：

- JDK：17 或以上
- Maven：3.9.x 或以上
- 可用端口：8080
- 支持 WebSocket 反向代理
- 具备文件导出能力（CSV / JSON / XML）
- 后续若接入真实模型服务，建议同时具备：
  - Python 3.10+
  - CUDA/昇腾/海光等对应驱动环境
  - 模型服务运行时与推理框架
