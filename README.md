# 多模态融合测试运行工具

本项目是“多模态融合测试运行工具”的完整实现，包含 **Vue 3 前端** 与 **Spring Boot 后端**，当前版本重点完成前后端通讯联调能力，包括 HTTP 接口、WebSocket 实时推送、心跳机制、任务流转、模型推理联调接口、数据流水线接口、资源监控、报告导出与用户审计等基础能力。

## 项目结构

### 前端（`frontend/`）

- [`frontend/src/views`](frontend/src/views)：页面视图（Dashboard、任务管理、模型管理、推理、监控、报告、用户管理等）
- [`frontend/src/services`](frontend/src/services)：API 与 WebSocket 服务封装
- [`frontend/src/router`](frontend/src/router)：Vue Router 路由配置
- [`frontend/src/stores`](frontend/src/stores)：Pinia 状态管理
- [`frontend/package.json`](frontend/package.json)：前端依赖与脚本

### 后端（`backend/`）

- [`backend/src/main/java/com/changan/multimodal/common`](backend/src/main/java/com/changan/multimodal/common)：公共能力，包括统一响应、异常处理、TraceId、线程池与 WebSocket 配置
- [`backend/src/main/java/com/changan/multimodal/task`](backend/src/main/java/com/changan/multimodal/task)：任务创建、启动、控制、流程状态管理
- [`backend/src/main/java/com/changan/multimodal/realtime`](backend/src/main/java/com/changan/multimodal/realtime)：WebSocket 通讯、会话管理、心跳与实时消息推送
- [`backend/src/main/java/com/changan/multimodal/inference`](backend/src/main/java/com/changan/multimodal/inference)：模型推理与评测接口
- [`backend/src/main/java/com/changan/multimodal/data`](backend/src/main/java/com/changan/multimodal/data)：多模态数据接入与增强流水线接口
- [`backend/src/main/java/com/changan/multimodal/monitor`](backend/src/main/java/com/changan/multimodal/monitor)：资源监控、阈值告警与实时推送
- [`backend/src/main/java/com/changan/multimodal/report`](backend/src/main/java/com/changan/multimodal/report)：报告聚合与导出
- [`backend/src/main/java/com/changan/multimodal/user`](backend/src/main/java/com/changan/multimodal/user)：用户登录统计与访问审计
- [`backend/src/main/resources/static/comm-test.html`](backend/src/main/resources/static/comm-test.html)：内置通讯联调测试页
- [`backend/docs`](backend/docs)：项目说明文档

## 先看这些文档

- 架构设计：[`ARCHITECTURE.md`](backend/docs/ARCHITECTURE.md)
- 前端通讯协议与联调说明：[`FRONTEND_PROTOCOL_GUIDE.md`](backend/docs/FRONTEND_PROTOCOL_GUIDE.md)
- 环境配置说明：[`ENVIRONMENT_SETUP.md`](backend/docs/ENVIRONMENT_SETUP.md)

## 快速启动

### 后端启动

1. 确认本地已安装 JDK 17+ 与 Maven
2. 进入 `backend` 目录：`cd backend`
3. 执行：`mvn spring-boot:run`
4. 服务默认地址：`http://localhost:8080`
5. 内置联调页面：`http://localhost:8080/comm-test.html`

### 前端启动

1. 确认本地已安装 Node.js 18+ 与 npm
2. 进入 `frontend` 目录：`cd frontend`
3. 安装依赖：`npm install`
4. 启动开发服务器：`npm run dev`
5. 前端默认地址：`http://localhost:5173`
6. 生产构建：`npm run build`

## 说明

当前版本以协议要求为基础，优先完成后端通讯骨架和联调能力；部分复杂业务逻辑已预留标准接口，后续可在不改变前后端协议的前提下继续扩展。
