package com.changan.multimodal.task.service;

import com.changan.multimodal.common.persistence.DemoPersistenceService;
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
import com.changan.multimodal.realtime.service.WsMessageRouter;
import com.changan.multimodal.task.dto.CreateTaskRequest;
import com.changan.multimodal.task.dto.RerunRequest;
import com.changan.multimodal.task.dto.TaskControlAction;
import com.changan.multimodal.task.dto.TaskControlRequest;
import com.changan.multimodal.task.dto.TaskInstance;
import com.changan.multimodal.task.dto.TaskNode;
import com.changan.multimodal.task.dto.TaskNodeRequest;
import com.changan.multimodal.task.dto.TaskNodeStatus;
import com.changan.multimodal.task.dto.TaskProgressEvent;
import com.changan.multimodal.task.dto.TaskStatus;
import com.changan.multimodal.task.dto.TaskTemplateRequest;
import com.changan.multimodal.task.dto.TaskTemplateView;
import com.changan.multimodal.task.dto.WorkflowValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final String DOMAIN = "TASK";
    private static final String TYPE_INSTANCE = "INSTANCE";
    private static final String TYPE_TEMPLATE = "TEMPLATE";

    private final Map<String, TaskInstance> taskStore = new ConcurrentHashMap<>();
    private final WsMessageRouter wsMessageRouter;
    private final ModelInferenceService modelInferenceService;
    private final DataPipelineService dataPipelineService;
    private final ResourceMonitorService resourceMonitorService;
    private final DemoPersistenceService persistenceService;
    @Qualifier("taskWorkflowExecutor")
    private final Executor taskWorkflowExecutor;

    public TaskInstance create(CreateTaskRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().toEpochMilli();
        TaskInstance instance = TaskInstance.builder()
                .taskId(taskId)
                .taskName(request.getTaskName())
                .scenarioDescription(request.getScenarioDescription())
                .status(TaskStatus.CREATED)
                .nodes(buildNodes(request.getNodes()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return saveInstance(instance);
    }

    public List<TaskInstance> list() {
        Map<String, TaskInstance> merged = new LinkedHashMap<>();
        persistenceService.findAll(DOMAIN, TYPE_INSTANCE, TaskInstance.class)
                .forEach(item -> merged.put(item.getTaskId(), item));
        taskStore.values().forEach(item -> merged.put(item.getTaskId(), item));
        return new ArrayList<>(merged.values());
    }

    public TaskInstance get(String taskId) {
        return requireTask(taskId);
    }

    public TaskInstance start(String taskId) {
        TaskInstance running = requireTask(taskId).withStatus(TaskStatus.RUNNING);
        WorkflowValidationResult validation = validateNodes(running.getNodes());
        if (!validation.isValid()) {
            TaskInstance failed = running.withStatus(TaskStatus.FAILED);
            saveInstance(failed);
            throw new IllegalArgumentException("流程校验失败: " + String.join("; ", validation.getErrors()));
        }
        saveInstance(running);
        taskWorkflowExecutor.execute(() -> simulateWorkflow(running));
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
        TaskInstance updated = saveInstance(current.withStatus(status));
        wsMessageRouter.broadcastTaskProgress(taskId, TaskProgressEvent.builder()
                .taskId(taskId)
                .taskStatus(status.name())
                .progress(TaskControlAction.TERMINATE.equals(request.getAction()) ? 0 : 50)
                .message("任务控制动作已受理: " + request.getAction().name())
                .build());
        return updated;
    }

    public WorkflowValidationResult validate(CreateTaskRequest request) {
        return validateNodes(buildNodes(request.getNodes()));
    }

    public WorkflowValidationResult simulate(CreateTaskRequest request) {
        WorkflowValidationResult validation = validate(request);
        if (!validation.isValid()) {
            return validation;
        }
        return WorkflowValidationResult.builder()
                .valid(true)
                .errors(List.of())
                .warnings(List.of("仿真通过：顺序、条件、并行依赖均可执行；资源分配为演示估算。"))
                .executionOrder(validation.getExecutionOrder())
                .build();
    }

    public TaskTemplateView saveTemplate(TaskTemplateRequest request) {
        String templateId = UUID.randomUUID().toString().replace("-", "");
        TaskTemplateView template = TaskTemplateView.builder()
                .templateId(templateId)
                .templateName(request.getTemplateName())
                .description(request.getDescription())
                .nodes(buildNodes(request.getNodes()))
                .createdAt(Instant.now().toEpochMilli())
                .build();
        return persistenceService.save(DOMAIN, TYPE_TEMPLATE, templateId, template);
    }

    public List<TaskTemplateView> listTemplates() {
        return persistenceService.findAll(DOMAIN, TYPE_TEMPLATE, TaskTemplateView.class);
    }

    public TaskInstance createFromTemplate(String templateId, CreateTaskRequest override) {
        TaskTemplateView template = persistenceService.findOne(DOMAIN, TYPE_TEMPLATE, templateId, TaskTemplateView.class)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));
        String taskId = UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().toEpochMilli();
        TaskInstance instance = TaskInstance.builder()
                .taskId(taskId)
                .taskName(override.getTaskName() == null ? template.getTemplateName() + "-复用任务" : override.getTaskName())
                .scenarioDescription(override.getScenarioDescription() == null ? template.getDescription() : override.getScenarioDescription())
                .status(TaskStatus.CREATED)
                .nodes(template.getNodes())
                .createdAt(now)
                .updatedAt(now)
                .build();
        return saveInstance(instance);
    }

    public TaskInstance rerun(String taskId, RerunRequest request) {
        TaskInstance current = requireTask(taskId);
        String mode = request.getMode() == null ? "FULL_CHAIN" : request.getMode();
        TaskInstance updated = current.withNodes(resetNodesForRerun(current.getNodes(), request.getNodeId(), mode))
                .withStatus(TaskStatus.RUNNING);
        saveInstance(updated);
        taskWorkflowExecutor.execute(() -> simulateWorkflow(updated));
        return updated;
    }

    public void simulateWorkflow(TaskInstance taskInstance) {
        try {
            List<TaskNode> nodes = new ArrayList<>(taskInstance.getNodes());
            int total = Math.max(nodes.size(), 1);
            for (int i = 0; i < nodes.size(); i++) {
                TaskNode runningNode = nodes.get(i).withStatus(TaskNodeStatus.RUNNING);
                nodes.set(i, runningNode);
                saveInstance(requireTask(taskInstance.getTaskId()).withNodes(List.copyOf(nodes)));
                wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                        .taskId(taskInstance.getTaskId())
                        .taskStatus(TaskStatus.RUNNING.name())
                        .nodeId(runningNode.getNodeId())
                        .nodeStatus(TaskNodeStatus.RUNNING.name())
                        .progress(i * 100 / total)
                        .message("节点开始执行，当前为演示流程运行")
                        .build());
                invokeNodeProtocol(taskInstance, runningNode);
                Thread.sleep(500L);

                TaskNode successNode = runningNode.withStatus(TaskNodeStatus.SUCCESS);
                nodes.set(i, successNode);
                saveInstance(requireTask(taskInstance.getTaskId()).withNodes(List.copyOf(nodes)));
                wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                        .taskId(taskInstance.getTaskId())
                        .taskStatus(TaskStatus.RUNNING.name())
                        .nodeId(successNode.getNodeId())
                        .nodeStatus(TaskNodeStatus.SUCCESS.name())
                        .progress((i + 1) * 100 / total)
                        .message(successNode.getTodo())
                        .build());
            }
            saveInstance(requireTask(taskInstance.getTaskId()).withStatus(TaskStatus.SUCCESS));
            wsMessageRouter.broadcastTaskProgress(taskInstance.getTaskId(), TaskProgressEvent.builder()
                    .taskId(taskInstance.getTaskId())
                    .taskStatus(TaskStatus.SUCCESS.name())
                    .progress(100)
                    .message("任务执行完成，结果已持久化")
                    .build());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            saveInstance(requireTask(taskInstance.getTaskId()).withStatus(TaskStatus.FAILED));
        }
    }

    private TaskInstance requireTask(String taskId) {
        TaskInstance taskInstance = taskStore.get(taskId);
        if (taskInstance == null) {
            taskInstance = persistenceService.findOne(DOMAIN, TYPE_INSTANCE, taskId, TaskInstance.class).orElse(null);
            if (taskInstance != null) {
                taskStore.put(taskId, taskInstance);
            }
        }
        if (taskInstance == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return taskInstance;
    }

    private TaskInstance saveInstance(TaskInstance instance) {
        taskStore.put(instance.getTaskId(), instance);
        return persistenceService.save(DOMAIN, TYPE_INSTANCE, instance.getTaskId(), instance);
    }

    private List<TaskNode> buildNodes(List<TaskNodeRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of(
                    node("data-access", "测试数据接入", "DATA", 1, 0.2, List.of(), "", "FAIL_FAST", "接入多模态数据源与元数据"),
                    node("model-invoke", "模型调用", "MODEL", 2, 0.5, List.of("data-access"), "data-access.SUCCESS", "FAIL_FAST", "调用本地模型进程并统一评测"),
                    node("report-export", "报告导出", "REPORT", 3, 0.1, List.of("model-invoke"), "model-invoke.SUCCESS", "USE_DEFAULT", "聚合报告、追溯与导出")
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
                        .dependencies(item.getDependencies() == null ? List.of() : item.getDependencies())
                        .conditionExpression(item.getConditionExpression())
                        .failureStrategy(item.getFailureStrategy() == null ? "FAIL_FAST" : item.getFailureStrategy())
                        .parameters(item.getParameters())
                        .todo("接入真实业务模块或演示适配器")
                        .build())
                .toList();
    }

    private TaskNode node(String id, String name, String type, int priority, double resourceRatio,
                          List<String> dependencies, String condition, String failureStrategy, String todo) {
        return TaskNode.builder()
                .nodeId(id)
                .nodeName(name)
                .nodeType(type)
                .status(TaskNodeStatus.PENDING)
                .priority(priority)
                .resourceRatio(resourceRatio)
                .dependencies(dependencies)
                .conditionExpression(condition)
                .failureStrategy(failureStrategy)
                .parameters(Map.of("demo", true))
                .todo(todo)
                .build();
    }

    private WorkflowValidationResult validateNodes(List<TaskNode> nodes) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> ids = new LinkedHashSet<>();
        for (TaskNode node : nodes) {
            if (node.getNodeId() == null || node.getNodeId().isBlank()) {
                errors.add("存在节点ID为空");
                continue;
            }
            if (!ids.add(node.getNodeId())) {
                errors.add("节点ID重复: " + node.getNodeId());
            }
            if (node.getNodeType() == null || node.getNodeType().isBlank()) {
                errors.add("节点类型为空: " + node.getNodeId());
            }
            if (node.getResourceRatio() != null && (node.getResourceRatio() < 0 || node.getResourceRatio() > 1)) {
                errors.add("资源占比需在0到1之间: " + node.getNodeId());
            }
        }
        for (TaskNode node : nodes) {
            for (String dependency : node.getDependencies() == null ? List.<String>of() : node.getDependencies()) {
                if (!ids.contains(dependency)) {
                    errors.add("节点依赖不存在: " + node.getNodeId() + " -> " + dependency);
                }
            }
            if ("USE_DEFAULT".equalsIgnoreCase(node.getFailureStrategy())) {
                warnings.add("节点 " + node.getNodeId() + " 配置为异常时使用默认结果替代");
            }
        }
        List<String> order = resolveExecutionOrder(nodes, errors);
        return WorkflowValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .executionOrder(order)
                .build();
    }

    private List<String> resolveExecutionOrder(List<TaskNode> nodes, List<String> errors) {
        Map<String, TaskNode> nodeMap = new LinkedHashMap<>();
        nodes.forEach(node -> nodeMap.put(node.getNodeId(), node));
        List<String> order = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (TaskNode node : nodes) {
            visit(node.getNodeId(), nodeMap, visiting, visited, order, errors);
        }
        return order;
    }

    private void visit(String nodeId, Map<String, TaskNode> nodeMap, Set<String> visiting, Set<String> visited,
                       List<String> order, List<String> errors) {
        if (nodeId == null || visited.contains(nodeId) || !nodeMap.containsKey(nodeId)) {
            return;
        }
        if (!visiting.add(nodeId)) {
            errors.add("存在环形依赖: " + nodeId);
            return;
        }
        TaskNode node = nodeMap.get(nodeId);
        for (String dependency : node.getDependencies() == null ? List.<String>of() : node.getDependencies()) {
            visit(dependency, nodeMap, visiting, visited, order, errors);
        }
        visiting.remove(nodeId);
        visited.add(nodeId);
        if (!order.contains(nodeId)) {
            order.add(nodeId);
        }
    }

    private List<TaskNode> resetNodesForRerun(List<TaskNode> nodes, String nodeId, String mode) {
        boolean resetAll = "FULL_CHAIN".equalsIgnoreCase(mode) || nodeId == null || nodeId.isBlank();
        Set<String> resetIds = new HashSet<>();
        if ("NODE_AND_DOWNSTREAM".equalsIgnoreCase(mode) && nodeId != null) {
            resetIds.add(nodeId);
            boolean changed;
            do {
                changed = false;
                for (TaskNode node : nodes) {
                    if (!resetIds.contains(node.getNodeId())
                            && node.getDependencies() != null
                            && node.getDependencies().stream().anyMatch(resetIds::contains)) {
                        changed = resetIds.add(node.getNodeId());
                    }
                }
            } while (changed);
        }
        List<TaskNode> result = new ArrayList<>();
        for (TaskNode node : nodes) {
            boolean shouldReset = resetAll
                    || ("SINGLE_NODE".equalsIgnoreCase(mode) && node.getNodeId().equals(nodeId))
                    || resetIds.contains(node.getNodeId());
            result.add(shouldReset ? node.withStatus(TaskNodeStatus.PENDING) : node);
        }
        return result;
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
                    .message("资源快照已采集 CPU=" + resourceMonitorService.currentSnapshot().getCpuUsage() + "%")
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
        Map<String, Object> parameters = node.getParameters() == null ? Map.of() : node.getParameters();
        List<DataAsset> assets = buildAssets(taskInstance, node, parameters.get("assets"));
        String datasetName = parameters.get("datasetName") == null
                ? taskInstance.getTaskName() + "-dataset"
                : parameters.get("datasetName").toString();
        DataIngestRequest request = new DataIngestRequest();
        request.setDatasetName(datasetName);
        request.setAssets(assets);
        request.setFilterRules(Map.of("taskId", taskInstance.getTaskId()));
        return request;
    }

    private List<DataAsset> buildAssets(TaskInstance taskInstance, TaskNode node, Object configuredAssets) {
        if (configuredAssets instanceof List<?> list && !list.isEmpty()) {
            List<DataAsset> assets = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    DataAsset asset = new DataAsset();
                    asset.setAssetId(valueAsString(map.get("assetId"), taskInstance.getTaskId() + "-asset-" + assets.size()));
                    asset.setUri(valueAsString(map.get("uri"), "sample-image.png"));
                    asset.setModality(valueAsString(map.get("modality"), "image"));
                    asset.setTags(toStringList(map.get("tags"), List.of("task", node.getNodeId())));
                    assets.add(asset);
                }
            }
            if (!assets.isEmpty()) {
                return assets;
            }
        }
        DataAsset asset = new DataAsset();
        asset.setAssetId(taskInstance.getTaskId() + "-asset");
        asset.setUri("sample-image.png");
        asset.setModality("image");
        asset.setTags(List.of("task", node.getNodeId()));
        return List.of(asset);
    }

    private DataPipelineRequest mockPipelineRequest(String datasetId) {
        DataPipelineOperation normalize = new DataPipelineOperation();
        normalize.setOperation("normalize");
        normalize.setParameters(Map.of("scale", "0-1"));
        DataPipelineOperation augment = new DataPipelineOperation();
        augment.setOperation("augment.cutmix");
        augment.setParameters(Map.of("ratio", 0.3));
        DataPipelineRequest request = new DataPipelineRequest();
        request.setDatasetId(datasetId);
        request.setOperations(List.of(normalize, augment));
        return request;
    }

    private InferenceRequest mockInferenceRequest(TaskNode node) {
        Map<String, Object> parameters = node.getParameters() == null ? Map.of() : node.getParameters();
        InferenceRequest request = new InferenceRequest();
        Object modelId = parameters.get("modelId");
        request.setModelId(modelId == null ? "yolov8-detection" : modelId.toString());
        request.setModality(valueAsString(parameters.get("modality"), "image"));
        request.setInputs(buildInferenceInputs(node, parameters.get("inputs")));
        request.setRequestedMetrics(toStringList(parameters.get("requestedMetrics"), List.of("mAP", "Precision", "Recall")));
        request.setOptions(Map.of("taskNodeId", node.getNodeId(), "flowTest", true));
        return request;
    }

    private List<InferenceInput> buildInferenceInputs(TaskNode node, Object configuredInputs) {
        if (configuredInputs instanceof List<?> list && !list.isEmpty()) {
            List<InferenceInput> inputs = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    InferenceInput input = new InferenceInput();
                    input.setInputId(valueAsString(map.get("inputId"), node.getNodeId() + "-input-" + inputs.size()));
                    input.setSourceUri(valueAsString(map.get("sourceUri"), "sample-image.png"));
                    input.setAttributes(toObjectMap(map.get("attributes"), Map.of("nodeId", node.getNodeId())));
                    inputs.add(input);
                }
            }
            if (!inputs.isEmpty()) {
                return inputs;
            }
        }
        InferenceInput input = new InferenceInput();
        input.setInputId(node.getNodeId() + "-input");
        input.setSourceUri("sample-image.png");
        input.setAttributes(Map.of("nodeId", node.getNodeId()));
        return List.of(input);
    }

    private String valueAsString(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private List<String> toStringList(Object value, List<String> fallback) {
        if (value instanceof List<?> list) {
            List<String> result = list.stream().map(String::valueOf).toList();
            return result.isEmpty() ? fallback : result;
        }
        return fallback;
    }

    private Map<String, Object> toObjectMap(Object value, Map<String, Object> fallback) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return fallback;
    }
}
