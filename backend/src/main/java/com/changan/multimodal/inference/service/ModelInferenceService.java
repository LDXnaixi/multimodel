package com.changan.multimodal.inference.service;

import com.changan.multimodal.inference.dto.EvaluationMetric;
import com.changan.multimodal.inference.dto.InferenceInput;
import com.changan.multimodal.inference.dto.InferenceOutput;
import com.changan.multimodal.inference.dto.InferenceRequest;
import com.changan.multimodal.inference.dto.InferenceResponse;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModelInferenceService {

    private final WsMessageRouter wsMessageRouter;

    public InferenceResponse runInference(InferenceRequest request) {
        long startedAt = Instant.now().toEpochMilli();
        String jobId = UUID.randomUUID().toString().replace("-", "");
        List<InferenceOutput> outputs = request.getInputs().stream()
                .map(this::mockOutput)
                .toList();
        List<EvaluationMetric> metrics = buildMetrics(request.getRequestedMetrics(), outputs);
        InferenceResponse response = InferenceResponse.builder()
                .jobId(jobId)
                .modelId(request.getModelId())
                .modality(request.getModality())
                .durationMs(Math.max(80, request.getInputs().size() * 35L))
                .outputs(outputs)
                .metrics(metrics)
                .status("COMPLETED")
                .build();
        wsMessageRouter.broadcast(WsMessageType.MODEL_RESULT, Map.of(
                "jobId", jobId,
                "modelId", request.getModelId(),
                "modality", request.getModality(),
                "finishedAt", Instant.now().toEpochMilli(),
                "startedAt", startedAt,
                "status", "COMPLETED"
        ));
        return response;
    }

    private InferenceOutput mockOutput(InferenceInput input) {
        double confidence = 0.72 + Math.abs(input.getInputId().hashCode() % 20) / 100.0;
        confidence = Math.min(confidence, 0.96);
        return InferenceOutput.builder()
                .inputId(input.getInputId())
                .label(resolveLabel(input.getSourceUri()))
                .confidence(confidence)
                .extra(Map.of(
                        "sourceUri", input.getSourceUri(),
                        "todo", "后续替换为真实模型服务RPC/HTTP调用",
                        "rawAttributes", input.getAttributes() == null ? Map.<String, Object>of() : input.getAttributes()
                ))
                .build();
    }

    private String resolveLabel(String sourceUri) {
        String lower = sourceUri.toLowerCase();
        if (lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".flac")) {
            return "speech.transcript";
        }
        if (lower.endsWith(".txt") || lower.endsWith(".json") || lower.endsWith(".csv")) {
            return "text.semantic";
        }
        return "vision.detected";
    }

    private List<EvaluationMetric> buildMetrics(List<String> requestedMetrics, List<InferenceOutput> outputs) {
        List<String> metrics = requestedMetrics == null || requestedMetrics.isEmpty()
                ? List.of("mAP", "Precision", "Recall")
                : requestedMetrics;
        double avgConfidence = outputs.stream().mapToDouble(InferenceOutput::getConfidence).average().orElse(0.8);
        List<EvaluationMetric> result = new ArrayList<>();
        for (String metric : metrics) {
            String normalized = metric.toLowerCase();
            double value = switch (normalized) {
                case "map" -> avgConfidence - 0.03;
                case "precision" -> avgConfidence;
                case "recall" -> Math.max(0.6, avgConfidence - 0.05);
                case "accuracy" -> avgConfidence + 0.01;
                case "latency" -> 120.0;
                default -> avgConfidence;
            };
            result.add(EvaluationMetric.builder()
                    .name(metric)
                    .value(Math.round(value * 10000.0) / 10000.0)
                    .unit("latency".equals(normalized) ? "ms" : "ratio")
                    .description("统一评测协议输出，可替换为真实模型评测结果")
                    .build());
        }
        return result;
    }
}
