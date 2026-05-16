package com.changan.multimodal.report.service;

import com.changan.multimodal.inference.dto.EvaluationMetric;
import com.changan.multimodal.monitor.dto.ResourceMetricSnapshot;
import com.changan.multimodal.monitor.service.ResourceMonitorService;
import com.changan.multimodal.report.dto.ReportSummary;
import com.changan.multimodal.task.dto.TaskInstance;
import com.changan.multimodal.task.dto.TaskNode;
import com.changan.multimodal.task.dto.TaskNodeStatus;
import com.changan.multimodal.task.service.TaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final TaskService taskService;
    private final ResourceMonitorService resourceMonitorService;
    private final ObjectMapper objectMapper;

    public ReportSummary getReport(String taskId) {
        TaskInstance task = taskService.get(taskId);
        int progress = task.getNodes().isEmpty() ? 0 : (int) task.getNodes().stream()
                .filter(node -> TaskNodeStatus.SUCCESS.equals(node.getStatus()))
                .count() * 100 / task.getNodes().size();
        ResourceMetricSnapshot snapshot = resourceMonitorService.currentSnapshot();
        return ReportSummary.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .taskStatus(task.getStatus().name())
                .progress(progress)
                .nodes(task.getNodes().stream().map(this::toNodeMap).toList())
                .metrics(List.of(
                        metric("precision", 0.91),
                        metric("recall", 0.88),
                        metric("throughput", snapshot.getNetworkThroughput())
                ))
                .resourceSnapshot(snapshot)
                .conclusion("当前报告由后端通讯骨架自动聚合生成，可直接供前端展示与导出")
                .build();
    }

    public ReportExport export(String taskId, String format) {
        ReportSummary report = getReport(taskId);
        String normalized = format.toUpperCase();
        return switch (normalized) {
            case "CSV" -> new ReportExport(taskId + ".csv", "text/csv;charset=UTF-8", toCsv(report).getBytes(StandardCharsets.UTF_8));
            case "XML" -> new ReportExport(taskId + ".xml", "application/xml;charset=UTF-8", toXml(report).getBytes(StandardCharsets.UTF_8));
            case "JSON" -> new ReportExport(taskId + ".json", "application/json;charset=UTF-8", toJson(report).getBytes(StandardCharsets.UTF_8));
            default -> throw new IllegalArgumentException("不支持的导出格式: " + format);
        };
    }

    private Map<String, Object> toNodeMap(TaskNode node) {
        return Map.<String, Object>of(
                "nodeId", node.getNodeId(),
                "nodeName", node.getNodeName(),
                "nodeType", node.getNodeType(),
                "status", node.getStatus().name(),
                "priority", node.getPriority(),
                "resourceRatio", node.getResourceRatio(),
                "todo", node.getTodo()
        );
    }

    private Map<String, Object> metric(String name, double value) {
        EvaluationMetric metric = EvaluationMetric.builder()
                .name(name)
                .value(value)
                .unit("ratio")
                .description("报告聚合指标")
                .build();
        return Map.<String, Object>of(
                "name", metric.getName(),
                "value", metric.getValue(),
                "unit", metric.getUnit(),
                "description", metric.getDescription()
        );
    }

    private String toJson(ReportSummary report) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON导出失败", ex);
        }
    }

    private String toXml(ReportSummary report) {
        try {
            return new XmlMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("XML导出失败", ex);
        }
    }

    private String toCsv(ReportSummary report) {
        StringBuilder builder = new StringBuilder();
        builder.append("taskId,taskName,taskStatus,progress\n");
        builder.append(report.getTaskId()).append(',')
                .append(report.getTaskName()).append(',')
                .append(report.getTaskStatus()).append(',')
                .append(report.getProgress()).append("\n\n");
        builder.append("nodeId,nodeName,nodeType,status,priority,resourceRatio\n");
        for (Map<String, Object> node : report.getNodes()) {
            builder.append(node.get("nodeId")).append(',')
                    .append(node.get("nodeName")).append(',')
                    .append(node.get("nodeType")).append(',')
                    .append(node.get("status")).append(',')
                    .append(node.get("priority")).append(',')
                    .append(node.get("resourceRatio")).append("\n");
        }
        return builder.toString();
    }

    public record ReportExport(String fileName, String contentType, byte[] content) {
    }
}
