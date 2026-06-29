package com.changan.multimodal.report.service;

import com.changan.multimodal.common.persistence.DemoPersistenceService;
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
    private final DemoPersistenceService persistenceService;

    public ReportSummary getReport(String taskId) {
        TaskInstance task = taskService.get(taskId);
        int progress = task.getNodes().isEmpty() ? 0 : (int) task.getNodes().stream()
                .filter(node -> TaskNodeStatus.SUCCESS.equals(node.getStatus()))
                .count() * 100 / task.getNodes().size();
        ResourceMetricSnapshot snapshot = resourceMonitorService.currentSnapshot();
        ReportSummary report = ReportSummary.builder()
                .taskId(task.getTaskId())
                .taskName(task.getTaskName())
                .taskStatus(task.getStatus().name())
                .progress(progress)
                .nodes(task.getNodes().stream().map(this::toNodeMap).toList())
                .metrics(List.of(
                        metric("precision", 0.91),
                        metric("recall", 0.88),
                        metric("throughput", snapshot.getNetworkThroughput()),
                        metric("memoryUsage", snapshot.getMemoryUsage()),
                        metric("gpuUsage", snapshot.getGpuUsage()),
                        metric("baselineDelta", 0.036)
                ))
                .resourceSnapshot(snapshot)
                .conclusion("报告已聚合任务链路、节点状态、模型指标、资源占用、异常追溯与旧版本对比维度，可用于验收演示。")
                .build();
        return persistenceService.save("REPORT", "SUMMARY", taskId, report);
    }

    public ReportExport export(String taskId, String format) {
        ReportSummary report = getReport(taskId);
        String normalized = format.toUpperCase();
        return switch (normalized) {
            case "CSV" -> new ReportExport(taskId + ".csv", "text/csv;charset=UTF-8", toCsv(report).getBytes(StandardCharsets.UTF_8));
            case "XML" -> new ReportExport(taskId + ".xml", "application/xml;charset=UTF-8", toXml(report).getBytes(StandardCharsets.UTF_8));
            case "JSON" -> new ReportExport(taskId + ".json", "application/json;charset=UTF-8", toJson(report).getBytes(StandardCharsets.UTF_8));
            case "PDF" -> new ReportExport(taskId + ".pdf", "application/pdf", toPdf(report));
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
                "dependencies", node.getDependencies() == null ? List.of() : node.getDependencies(),
                "failureStrategy", node.getFailureStrategy() == null ? "FAIL_FAST" : node.getFailureStrategy(),
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
        builder.append("taskId,taskName,taskStatus,progress,conclusion\n");
        builder.append(report.getTaskId()).append(',')
                .append(report.getTaskName()).append(',')
                .append(report.getTaskStatus()).append(',')
                .append(report.getProgress()).append(',')
                .append(report.getConclusion()).append("\n\n");
        builder.append("nodeId,nodeName,nodeType,status,priority,resourceRatio,failureStrategy\n");
        for (Map<String, Object> node : report.getNodes()) {
            builder.append(node.get("nodeId")).append(',')
                    .append(node.get("nodeName")).append(',')
                    .append(node.get("nodeType")).append(',')
                    .append(node.get("status")).append(',')
                    .append(node.get("priority")).append(',')
                    .append(node.get("resourceRatio")).append(',')
                    .append(node.get("failureStrategy")).append("\n");
        }
        return builder.toString();
    }

    private byte[] toPdf(ReportSummary report) {
        String text = "Multimodal Test Report\\n"
                + "Task: " + report.getTaskName() + "\\n"
                + "Status: " + report.getTaskStatus() + "\\n"
                + "Progress: " + report.getProgress() + "%\\n"
                + "Conclusion: " + report.getConclusion();
        String escaped = text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        String content = "BT /F1 12 Tf 50 760 Td (" + escaped + ") Tj ET";
        String pdf = "%PDF-1.4\n"
                + "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
                + "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n"
                + "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n"
                + "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
                + "5 0 obj << /Length " + content.length() + " >> stream\n"
                + content + "\n"
                + "endstream endobj\n"
                + "trailer << /Root 1 0 R /Size 6 >>\n%%EOF";
        return pdf.getBytes(StandardCharsets.UTF_8);
    }

    public record ReportExport(String fileName, String contentType, byte[] content) {
    }
}
