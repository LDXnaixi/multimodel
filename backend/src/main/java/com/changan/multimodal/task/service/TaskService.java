package com.changan.multimodal.task.service;

import com.changan.multimodal.realtime.service.WsMessageRouter;
import com.changan.multimodal.data.dto.DataAsset;
import com.changan.multimodal.data.dto.DataIngestRequest;
import com.changan.multimodal.data.dto.DataIngestResponse;
import com.changan.multimodal.data.dto.DataPipelineOperation;
import com.changan.multimodal.data.dto.DataPipelineRequest;
import com.changan.multimodal.data.dto.DataPipelineResponse;
import com.changan.multimodal.data.service.DataPipelineService;
import com.changan.multimodal.inference.dto.InferenceInput;
import com.changan.multimodal.inference.dto.InferenceRequest;
import com.changan.multimodal.inference.dto.InferenceResponse;
import com.changan.multimodal.inference.service.ModelInferenceService;
import com.changan.multimodal.monitor.service.ResourceMonitorService;
import com.changan.multimodal.task.dto.CreateTaskRequest;
import com.changan.multimodal.task.dto.TaskControlAction;
import com.changan.multimodal.task.dto.TaskControlRequest;
import com.changan.multimodal.task.dto.TaskInstance;
import com.changan.multimodal.task.dto.TaskNode;
import com.changan.multimodal.task.dto.TaskNodeRequest;
import com.changan.multimodal.task.dto.TaskNodeStatus;
import com.changan.multimodal.task.dto.TaskProgressEvent;
import com.changan.multimodal.task.dto.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final Map<String, TaskInstance> taskStore = new ConcurrentHashMap<>();
    private final WsMessageRouter wsMessageRouter;
    private final ModelInferenceService modelInferenceService;
    private final DataPipelineService dataPipelineService;
    private final ResourceMonitorService resourceMonitorService;

    public TaskInstance create(CreateTaskRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().toEpochMilli();
        List<TaskNode> nodes = buildNodes(request.getNodes());
        TaskInstance instance = TaskInstance.builder()
                .taskId(taskId)
                .taskName(request.getTaskName())
                .scenarioDescription(request.getScenarioDescription())
                .status(TaskStatus.CREATED)
                .nodes(nodes)
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskStore.put(taskId, instance);
        return instance;
    }

    public List<TaskInstance> list() {
        return new ArrayList<>(taskStore.values());
    }

    public TaskInstance get(String taskId) {
        return requireTask(taskId);
    }

    public TaskInstance start(String taskId) {
        TaskInstance running = requireTask(taskId).withStatus(TaskStatus.RUNNING);
        taskStore.put(taskId, running);
        simulateWorkflow(running);
        return running;
    }

    public TaskInstance control(String taskId, TaskControlRequest request) {
        TaskInstance current = requireTask(taskId);
        TaskStatus status = switch (request.getAction()) {
            case PAUSE -> TaskStatus.PAUSED;
            case RESUME -> TaskStatus.RUNNING;
            case RERUN -> TaskStatus.RUNNING;
            case TERMINATE -> TaskStatus.TERMINATED;
        };
        TaskInstance updated = current.withStatus(status);
        taskStore.put(taskId, updated);
        wsMessageRouter.broadcastTaskProgress(taskId, TaskProgressEvent.builder()
                .taskId(taskId)
                .taskStatus(status.name())
                .progress(TaskControlAction.TERMINATE.equals(request.getAction()) ? 0 : 50)
                .message("任务控制动作已受理: " + request.getAction().name())
                .build());
        return updated;
    }

    @Async("taskWorkflowExecutor")
    public void simulateWorkflow(TaskInstance taskInstance) {
        try {
            List<TaskNode> nodes = new ArrayList<>(taskInstance.getNodes());
            int total = Math.max(nodes.size(), 1);
            for (int i = 0; i < nodes.size(); i++) {
                TaskNode runningNode = nodes.get(i).withStatus(TaskNodeStatus.RUNNING);
                nodes.set(i, runningNode);
                taskStore.computeIfPresent(taskInstance.getTaskId(), (k, v) -> v.withNodes(List.copyOf(nodes)));
                wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                        .taskId(taskInstance.getTaskId())
                        .taskStatus(TaskStatus.RUNNING.name())
                        .nodeId(runningNode.getNodeId())
                        .nodeStatus(TaskNodeStatus.RUNNING.name())
                        .progress(i * 100 / total)
                        .message("节点开始执行，当前为通讯联调模拟流程")
                        .build());
                invokeNodeProtocol(taskInstance, runningNode);
                Thread.sleep(800L);

                TaskNode successNode = runningNode.withStatus(TaskNodeStatus.SUCCESS);
                nodes.set(i, successNode);
                taskStore.computeIfPresent(taskInstance.getTaskId(), (k, v) -> v.withNodes(List.copyOf(nodes)));
                wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                        .taskId(taskInstance.getTaskId())
                        .taskStatus(TaskStatus.RUNNING.name())
                        .nodeId(successNode.getNodeId())
                        .nodeStatus(TaskNodeStatus.SUCCESS.name())
                        .progress((i + 1) * 100 / total)
                        .message(successNode.getTodo())
                        .build());
            }
            taskStore.computeIfPresent(taskInstance.getTaskId(), (k, v) -> v.withStatus(TaskStatus.SUCCESS));
            wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                    .taskId(taskInstance.getTaskId())
                    .taskStatus(TaskStatus.SUCCESS.name())
                    .progress(100)
                    .message("任务执行完成（当前为联调占位实现）")
                    .build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            taskStore.computeIfPresent(taskInstance.getTaskId(), (k, v) -> v.withStatus(TaskStatus.FAILED));
        }
    }

    private TaskInstance requireTask(String taskId) {
        TaskInstance taskInstance = taskStore.get(taskId);
        if (taskInstance == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return taskInstance;
    }

    private List<TaskNode> buildNodes(List<TaskNodeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of(
                    TaskNode.builder()
                            .nodeId("data-access")
                            .nodeName("测试数据接入")
                            .nodeType("DATA")
                            .status(TaskNodeStatus.PENDING)
                            .priority(1)
                            .resourceRatio(0.2)
                            .parameters(Map.<String, Object>of("todo", true))
                            .todo("待接入真实多模态数据源与元数据管理")
                            .build(),
                    TaskNode.builder()
                            .nodeId("model-invoke")
                            .nodeName("模型调用")
                            .nodeType("MODEL")
                            .status(TaskNodeStatus.PENDING)
                            .priority(2)
                            .resourceRatio(0.5)
                            .parameters(Map.<String, Object>of("todo", true))
                            .todo("待接入真实模型服务编排与统一评测接口")
                            .build(),
                    TaskNode.builder()
                            .nodeId("report-export")
                            .nodeName("报告导出")
                            .nodeType("REPORT")
                            .status(TaskNodeStatus.PENDING)
                            .priority(3)
                            .resourceRatio(0.1)
                            .parameters(Map.<String, Object>of("todo", true))
                            .todo("待接入真实报告聚合、追溯与导出能力")
                            .build()
            );
        }
        return requests.stream()
                .map(item -> TaskNode.builder()
                        .nodeId(item.getNodeId())
                        .nodeName(item.getNodeName())
                        .nodeType(item.getNodeType())
                        .status(TaskNodeStatus.PENDING)
                        .priority(item.getPriority())
                        .resourceRatio(item.getResourceRatio())
                        .parameters(item.getParameters())
                        .todo("待接入真实业务模块")
                        .build())
                .toList();
    }

    private void invokeNodeProtocol(TaskInstance taskInstance, TaskNode node) {
        switch (node.getNodeType()) {
            case "DATA" -> {
                DataIngestResponse dataset = dataPipelineService.registerDataset(mockDatasetRequest(taskInstance, node));
                DataPipelineResponse pipeline = dataPipelineService.runPipeline(mockPipelineRequest(dataset.getDatasetId()));
                wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                        .taskId(taskInstance.getTaskId())
                        .taskStatus(TaskStatus.RUNNING.name())
                        .nodeId(node.getNodeId())
                        .nodeStatus(TaskNodeStatus.RUNNING.name())
                        .progress(20)
                        .message("数据流水线完成: " + pipeline.getExecutedOperations())
                        .build());
            }
            case "MODEL" -> {
                InferenceResponse response = modelInferenceService.runInference(mockInferenceRequest(node));
                wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                        .taskId(taskInstance.getTaskId())
                        .taskStatus(TaskStatus.RUNNING.name())
                        .nodeId(node.getNodeId())
                        .nodeStatus(TaskNodeStatus.RUNNING.name())
                        .progress(60)
                        .message("模型推理完成，输出条数: " + response.getOutputs().size())
                        .build());
            }
            case "REPORT" -> wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                    .taskId(taskInstance.getTaskId())
                    .taskStatus(TaskStatus.RUNNING.name())
                    .nodeId(node.getNodeId())
                    .nodeStatus(TaskNodeStatus.RUNNING.name())
                    .progress(80)
                    .message("资源快照已采集: CPU=" + resourceMonitorService.currentSnapshot().getCpuUsage() + "%")
                    .build());
            default -> wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                    .taskId(taskInstance.getTaskId())
                    .taskStatus(TaskStatus.RUNNING.name())
                    .nodeId(node.getNodeId())
                    .nodeStatus(TaskNodeStatus.RUNNING.name())
                    .progress(30)
                    .message("通用节点执行中")
                    .build());
        }
    }

    private DataIngestRequest mockDatasetRequest(TaskInstance taskInstance, TaskNode node) {
        DataAsset asset = new DataAsset();
        asset.setAssetId(taskInstance.getTaskId() + "-asset");
        asset.setUri("sample-image.png");
        asset.setModality("image");
        asset.setTags(List.of("task", node.getNodeId()));
        DataIngestRequest request = new DataIngestRequest();
        request.setDatasetName(taskInstance.getTaskName() + "-dataset");
        request.setAssets(List.of(asset));
        request.setFilterRules(Map.<String, Object>of("taskId", taskInstance.getTaskId()));
        return request;
    }

    private DataPipelineRequest mockPipelineRequest(String datasetId) {
        DataPipelineOperation normalize = new DataPipelineOperation();
        normalize.setOperation("normalize");
        normalize.setParameters(Map.<String, Object>of("scale", "0-1"));
        DataPipelineOperation augment = new DataPipelineOperation();
        augment.setOperation("augment.cutmix");
        augment.setParameters(Map.<String, Object>of("ratio", 0.3));
        DataPipelineRequest request = new DataPipelineRequest();
        request.setDatasetId(datasetId);
        request.setOperations(List.of(normalize, augment));
        return request;
    }

    private InferenceRequest mockInferenceRequest(TaskNode node) {
        InferenceInput input = new InferenceInput();
        input.setInputId(node.getNodeId() + "-input");
        input.setSourceUri("sample-image.png");
        input.setAttributes(Map.<String, Object>of("nodeId", node.getNodeId()));
        InferenceRequest request = new InferenceRequest();
        request.setModelId("yolov8-demo");
        request.setModality("image");
        request.setInputs(List.of(input));
        request.setRequestedMetrics(List.of("mAP", "Precision", "Recall"));
        request.setOptions(Map.<String, Object>of("todo", false));
        return request;
    }
}
