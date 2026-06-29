# 多模态融合测试运行工具

本项目是“多模态融合测试运行工具”的完整实现，包含 **Vue 3 前端** 与 **Spring Boot 后端**，当前版本重点完成前后端通讯联调能力，包括 HTTP 接口、WebSocket 实时推送、心跳机制、任务流转、模型推理联调接口、数据流水线接口、资源监控、报告导出与用户审计等基础能力。

## 数据集与模型推理通讯

浏览器不再提交服务器文件路径。用户选择包含图片和标注的数据集文件夹，前端通过 `multipart/form-data` 同时上传文件及相对路径。后端按同名主文件名配对，例如 `images/train/a.jpg` 对应 `labels/train/a.txt`。MySQL/H2 的 `data_datasets`、`data_samples` 表分别保存数据集，以及每张图片和对应标签的名称、相对路径、类型、大小、SHA-256 与受管存储键。模型推理请求只提交 `sampleId`，后端校验样本后才把图片和标签真实路径交给本地模型进程，响应中会移除内部路径字段。

主要接口：

- `POST /api/v1/data/datasets/upload`：上传文件夹并创建图片—标签配对数据集。
- `GET /api/v1/data/datasets`：读取服务器已有数据集。
- `GET /api/v1/data/datasets/{datasetId}/samples`：读取数据集样本。
- `GET /api/v1/data/samples/{sampleId}/content`：受控预览或读取样本内容。
- `GET /api/v1/data/samples/{sampleId}/label`：读取该图片对应的标签。
- `POST /api/v1/inference/run`：使用 `inputs[].sampleId` 执行推理。

数据集清单以 HTTP 查询结果为准。上传成功后，服务器广播 `DATA_CATALOG_CHANGED` WebSocket 消息，在线客户端收到消息后重新拉取清单；即使 WebSocket 断线，页面重新进入或点击刷新也能恢复完整状态。

样本文件默认保存在 `./data/sample-assets`，生产环境建议把它指向服务器 4TB SSD 的独立数据目录：

```powershell
$env:APP_SAMPLE_STORAGE_ROOT='D:\multimodal-data\sample-assets'
```

数据库中只保存相对 `storageKey`，不会保存或返回部署机器的绝对路径。多节点部署时可将受管存储实现替换成 MinIO，`sampleId` 和前端协议无需改变。

当前文件夹数据集规则：

- 图片格式：PNG、JPG/JPEG、BMP、GIF、WebP。
- 标签格式：TXT、JSON、XML、CSV、YAML/YML。
- 图片与标签去掉扩展名后必须同名。
- 支持图片、标签混放，也支持 `images/...` 与 `labels/...` 分目录。
- 缺少标签、孤立标签或同名歧义会拒绝整个数据集，数据库事务和已写文件会一起回滚。

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
