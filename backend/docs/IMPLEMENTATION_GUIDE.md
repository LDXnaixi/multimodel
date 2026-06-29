# 二次开发与模型集成指南

## MySQL 配置

默认读取以下环境变量：

```bash
APP_DATASOURCE_URL=jdbc:mysql://localhost:3306/multimodal_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
APP_DATASOURCE_USERNAME=root
APP_DATASOURCE_PASSWORD=root
```

首次启动会自动创建演示持久化表 `demo_records`。当前版本为了兼容演示迭代，使用领域 JSON 快照保存模型、任务、样本、报告、监控、审计等数据。

数据文件采用独立的受管存储方案。Hibernate 会另外创建 `data_datasets` 和 `data_samples`，前者保存数据集清单，后者在同一行保存图片与对应标签双方的相对路径、内容类型、大小、SHA-256 与相对存储键；原文件不存入数据库 BLOB。

```powershell
APP_SAMPLE_STORAGE_ROOT=D:\multimodal-data\sample-assets
```

客户端通过 `POST /api/v1/data/datasets/upload` 上传整个文件夹。表单中 `files` 与 `relativePaths` 必须按相同顺序重复出现。后端会将 `images/train/a.jpg` 与 `labels/train/a.txt` 配成一条样本；缺图、缺标签或重名歧义时整个请求失败。客户端通过 `GET /api/v1/data/datasets` 和 `GET /api/v1/data/datasets/{id}/samples` 获取服务器目录。推理请求应提交 `sampleId`：

```json
{
  "modelId": "yolov8-detection",
  "modality": "image",
  "inputs": [
    {
      "inputId": "可选客户端关联ID",
      "sampleId": "服务器返回的样本ID",
      "attributes": {
        "datasetId": "服务器返回的数据集ID"
      }
    }
  ],
  "requestedMetrics": ["mAP", "Precision", "Recall"],
  "options": {"batchSize": 1}
}
```

后端仅在调用模型进程前解析真实文件路径，并会清理模型输出中的 `sourceUri`、`storageKey`、`labelUri`，避免把服务器目录结构泄漏给浏览器。`DATA_CATALOG_CHANGED` WebSocket 消息是刷新通知，HTTP 清单接口始终是权威数据源。

## 本地模型进程协议

后端默认运行：

```bash
python ./model-runners/demo_model_runner.py
```

模型进程从 `stdin` 读取统一 JSON：

```json
{
  "modelId": "yolov8-detection",
  "modality": "image",
  "inputs": [
    {"inputId": "sample-id", "sampleId": "sample-id", "sourceUri": "/resolved/server/path/image.png", "attributes": {}}
  ],
  "requestedMetrics": ["mAP", "Precision", "Recall"],
  "options": {}
}
```

这里的 `sourceUri` 是后端根据 `sampleId` 解析后写给模型进程的内部路径，不是浏览器提交字段。模型进程不应在输出中回传它。

模型进程向 `stdout` 输出：

```json
{
  "outputs": [
    {"inputId": "img-001", "label": "object.detected", "confidence": 0.91, "extra": {}}
  ],
  "metrics": [
    {"name": "mAP", "value": 0.86, "unit": "ratio", "description": "metric"}
  ],
  "durationMs": 80,
  "runnerStatus": "COMPLETED"
}
```

如果进程超时、退出码非 0 或输出不是 JSON，后端会自动使用确定性演示备用结果，并记录运行日志。

## 关键接口

- `POST /api/v1/models`：注册本地模型进程
- `POST /api/v1/models/{modelId}/status`：上线/下线
- `POST /api/v1/models/{modelId}/rollback`：版本回滚
- `GET /api/v1/models/run-logs`：模型运行日志
- `POST /api/v1/tasks/validate`：流程校验
- `POST /api/v1/tasks/simulate`：流程仿真
- `POST /api/v1/tasks/templates`：保存模板
- `POST /api/v1/tasks/{taskId}/rerun`：节点级重跑
- `GET /api/v1/reports/{taskId}/export?format=PDF`：PDF 报告
- `POST /api/v1/environment/capture`：采集环境一致性报告
