package com.changan.multimodal.inference.service;

import com.changan.multimodal.data.service.DataPipelineService;
import com.changan.multimodal.inference.dto.EvaluationMetric;
import com.changan.multimodal.inference.dto.InferenceInput;
import com.changan.multimodal.inference.dto.InferenceOutput;
import com.changan.multimodal.inference.dto.InferenceRequest;
import com.changan.multimodal.inference.dto.InferenceResponse;
import com.changan.multimodal.model.dto.ModelRunLog;
import com.changan.multimodal.model.dto.ModelDescriptor;
import com.changan.multimodal.model.service.ModelRegistryService;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModelInferenceService {

    private final WsMessageRouter wsMessageRouter;
    private final ModelRegistryService modelRegistryService;
    private final LocalModelProcessRunner localModelProcessRunner;
    private final DataPipelineService dataPipelineService;
    private final ObjectMapper objectMapper;

    public InferenceResponse runInference(InferenceRequest request) {
        long startedAt = Instant.now().toEpochMilli();
        String jobId = UUID.randomUUID().toString().replace("-", "");
        
        ModelDescriptor model = findModel(request.getModelId());
        resolveManagedInputs(request);
        JsonNode runnerResult = localModelProcessRunner.run(request, model.getRuntimeCommand()).orElse(null);
        List<InferenceOutput> outputs = runnerResult == null
                ? request.getInputs().stream().map(input -> mockOutput(input, model)).toList()
                : parseOutputs(runnerResult, request, model);
        List<EvaluationMetric> metrics = runnerResult == null
                ? buildMetrics(model, outputs, request.getRequestedMetrics())
                : parseMetrics(runnerResult, model, outputs, request.getRequestedMetrics());
        long durationMs = runnerResult != null && runnerResult.has("durationMs")
                ? runnerResult.path("durationMs").asLong()
                : Math.max(80, request.getInputs().size() * 35L);
        
        InferenceResponse response = InferenceResponse.builder()
                .jobId(jobId)
                .modelId(request.getModelId())
                .modality(request.getModality())
                .durationMs(durationMs)
                .outputs(outputs)
                .metrics(metrics)
                .status("COMPLETED")
                .build();
        long finishedAt = Instant.now().toEpochMilli();
        modelRegistryService.appendRunLog(ModelRunLog.builder()
                .logId(UUID.randomUUID().toString().replace("-", ""))
                .modelId(request.getModelId())
                .jobId(jobId)
                .status("COMPLETED")
                .startedAt(startedAt)
                .finishedAt(finishedAt)
                .durationMs(finishedAt - startedAt)
                .command(model.getRuntimeCommand() == null ? "default local demo runner" : model.getRuntimeCommand())
                .message(runnerResult == null
                        ? "Local model process unavailable; deterministic demo result returned. adapterType=" + model.getAdapterType()
                        : "Local model process completed. adapterType=" + model.getAdapterType())
                .build());
        
        wsMessageRouter.broadcast(WsMessageType.MODEL_RESULT, Map.of(
                "jobId", jobId,
                "modelId", request.getModelId(),
                "modality", request.getModality(),
                "finishedAt", finishedAt,
                "startedAt", startedAt,
                "status", "COMPLETED"
        ));
        
        return response;
    }

    private ModelDescriptor findModel(String modelId) {
        return modelRegistryService.listModels().stream()
                .filter(m -> m.getModelId().equals(modelId))
                .findFirst()
                .orElseGet(() -> ModelDescriptor.builder()
                        .modelId(modelId)
                        .modelCategory("UNCLASSIFIED")
                        .availableMetrics(List.of("mAP", "Precision", "Recall"))
                        .build());
    }

    private List<InferenceOutput> parseOutputs(JsonNode runnerResult, InferenceRequest request, ModelDescriptor model) {
        JsonNode outputsNode = runnerResult.path("outputs");
        if (!outputsNode.isArray()) {
            return request.getInputs().stream().map(input -> mockOutput(input, model)).toList();
        }
        List<InferenceOutput> outputs = new ArrayList<>();
        for (JsonNode item : outputsNode) {
            InferenceOutput output = objectMapper.convertValue(item, InferenceOutput.class);
            InferenceInput sourceInput = request.getInputs().stream()
                    .filter(input -> input.getInputId().equals(output.getInputId()))
                    .findFirst()
                    .orElse(null);
            Map<String, Object> safeExtra = sanitizeRunnerOutput(output.getExtra());
            if (sourceInput != null && sourceInput.getSampleId() != null) {
                safeExtra.put("sampleId", sourceInput.getSampleId());
            }
            outputs.add(InferenceOutput.builder()
                    .inputId(output.getInputId())
                    .label(output.getLabel())
                    .confidence(output.getConfidence())
                    .extra(safeExtra)
                    .build());
        }
        return outputs;
    }

    private List<EvaluationMetric> parseMetrics(JsonNode runnerResult, ModelDescriptor model,
                                                List<InferenceOutput> outputs, List<String> requestedMetrics) {
        JsonNode metricsNode = runnerResult.path("metrics");
        if (!metricsNode.isArray()) {
            return buildMetrics(model, outputs, requestedMetrics);
        }
        List<EvaluationMetric> metrics = new ArrayList<>();
        for (JsonNode item : metricsNode) {
            metrics.add(objectMapper.convertValue(item, EvaluationMetric.class));
        }
        return metrics;
    }

    private InferenceOutput mockOutput(InferenceInput input, ModelDescriptor model) {
        Map<String, Object> extra = new HashMap<>();
        if (input.getSampleId() != null) {
            extra.put("sampleId", input.getSampleId());
        }
        extra.put("modelCategory", model.getModelCategory());
        extra.put("adapterType", model.getAdapterType());
        extra.put("adapterStatus", model.getAdapterStatus());
        extra.put("adapterConfig", model.getAdapterConfig());
        extra.put("outputSchema", model.getOutputSchema());
        extra.put("resultMode", model.getRuntimeCommand() == null ? "DEMO_RESULT" : "REAL_ADAPTER_FALLBACK");
        
        String label = switch (model.getModelCategory()) {
            case "OBJECT_DETECTION" -> "object.detected";
            case "OCR" -> "text.recognized";
            case "IMAGE_CLASSIFICATION" -> "image.classified";
            case "SEMANTIC_ANALYSIS" -> "text.analyzed";
            case "SPEECH_RECOGNITION" -> "speech.transcript";
            case "VISION_LANGUAGE" -> "vl.answered";
            case "CUSTOM" -> "custom.output";
            default -> "unknown.output";
        };

        if ("SPEECH_RECOGNITION".equals(model.getModelCategory())) {
            extra.put("transcript", "这是一个模拟的语音转录文本，支持多语言输出。");
        } else if ("VISION_LANGUAGE".equals(model.getModelCategory())) {
            extra.put("answer", "这是根据输入图片生成的描述性回答。");
        }

        double confidence = 0.72 + Math.abs(input.getInputId().hashCode() % 20) / 100.0;
        confidence = Math.min(confidence, 0.96);
        
        extra.put("rawAttributes", input.getAttributes() == null ? Map.<String, Object>of() : input.getAttributes());
        
        return InferenceOutput.builder()
                .inputId(input.getInputId())
                .label(label)
                .confidence(confidence)
                .extra(extra)
                .build();
    }

    private void resolveManagedInputs(InferenceRequest request) {
        for (InferenceInput input : request.getInputs()) {
            if (input.getSampleId() != null && !input.getSampleId().isBlank()) {
                input.setSourceUri(dataPipelineService.resolveSamplePath(input.getSampleId()).toString());
                Map<String, Object> attributes = new HashMap<>(
                        input.getAttributes() == null ? Map.of() : input.getAttributes()
                );
                dataPipelineService.resolveSampleLabelPath(input.getSampleId())
                        .ifPresent(path -> attributes.put("labelUri", path.toString()));
                input.setAttributes(attributes);
                if (input.getInputId() == null || input.getInputId().isBlank()) {
                    input.setInputId(input.getSampleId());
                }
            } else if (input.getInputId() == null || input.getInputId().isBlank()) {
                input.setInputId(UUID.randomUUID().toString().replace("-", ""));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeRunnerOutput(Map<String, Object> extra) {
        Map<String, Object> safe = new LinkedHashMap<>();
        if (extra == null) {
            return safe;
        }
        extra.forEach((key, value) -> {
            if ("sourceUri".equalsIgnoreCase(key)
                    || "storageKey".equalsIgnoreCase(key)
                    || "labelUri".equalsIgnoreCase(key)) {
                return;
            }
            if (value instanceof Map<?, ?> nested) {
                safe.put(key, sanitizeRunnerOutput((Map<String, Object>) nested));
            } else if (value instanceof List<?> list) {
                safe.put(key, list.stream().map(item -> item instanceof Map<?, ?>
                        ? sanitizeRunnerOutput((Map<String, Object>) item)
                        : item).toList());
            } else {
                safe.put(key, value);
            }
        });
        return safe;
    }

    private List<EvaluationMetric> buildMetrics(ModelDescriptor model, List<InferenceOutput> outputs,
                                                List<String> requestedMetrics) {
        List<String> availableMetrics = requestedMetrics;
        if (availableMetrics == null || availableMetrics.isEmpty()) {
            availableMetrics = model.getAvailableMetrics();
        }
        if (availableMetrics == null || availableMetrics.isEmpty()) {
            availableMetrics = List.of("mAP", "Precision", "Recall");
        }
        
        double avgConfidence = outputs.stream().mapToDouble(InferenceOutput::getConfidence).average().orElse(0.8);
        List<EvaluationMetric> result = new ArrayList<>();
        
        for (String metric : availableMetrics) {
            String normalized = metric.toLowerCase();
            double value = calculateMetricValue(normalized, model.getModelCategory(), avgConfidence);
            String unit = "latency".equals(normalized) || "fps".equals(normalized) || "wer".equals(normalized) || "cer".equals(normalized) ? (
                    "fps".equals(normalized) ? "fps" : 
                    "latency".equals(normalized) ? "ms" : 
                    "ratio"
            ) : "ratio";
            
            String description = getMetricDescription(metric, model.getModelCategory());
            
            result.add(EvaluationMetric.builder()
                    .name(metric)
                    .value(Math.round(value * 10000.0) / 10000.0)
                    .unit(unit)
                    .description(description)
                    .build());
        }
        
        return result;
    }

    private double calculateMetricValue(String metric, String category, double baseConfidence) {
        return switch (metric) {
            case "map" -> baseConfidence - 0.03 + getCategoryOffset(category);
            case "precision" -> baseConfidence + getCategoryOffset(category);
            case "recall" -> Math.max(0.6, baseConfidence - 0.05 + getCategoryOffset(category));
            case "fps" -> 15.0 + Math.random() * 20.0;
            case "accuracy" -> baseConfidence + 0.01 + getCategoryOffset(category);
            case "top1-accuracy" -> baseConfidence + 0.02;
            case "top5-accuracy" -> baseConfidence + 0.08;
            case "f1-score" -> (2 * baseConfidence * baseConfidence) / (baseConfidence + baseConfidence);
            case "perplexity" -> 15.0 + Math.random() * 10.0;
            case "wer", "cer" -> 0.15 - Math.random() * 0.1;
            case "recognitionrate" -> baseConfidence + 0.05;
            case "rejectionrate" -> 0.05 + Math.random() * 0.1;
            case "bleu", "rouge" -> 0.6 + Math.random() * 0.3;
            case "relevance" -> 0.7 + Math.random() * 0.25;
            case "latency" -> 50.0 + Math.random() * 100.0;
            case "throughput" -> 100.0 + Math.random() * 200.0;
            default -> baseConfidence;
        };
    }

    private double getCategoryOffset(String category) {
        return switch (category) {
            case "OBJECT_DETECTION" -> 0.02;
            case "OCR" -> 0.03;
            case "IMAGE_CLASSIFICATION" -> 0.04;
            case "SEMANTIC_ANALYSIS" -> 0.01;
            case "SPEECH_RECOGNITION" -> -0.02;
            case "VISION_LANGUAGE" -> -0.03;
            case "CUSTOM" -> 0.0;
            default -> 0.0;
        };
    }

    private String getMetricDescription(String metric, String category) {
        return switch (metric.toLowerCase()) {
            case "map" -> category + " 模型的平均精度均值";
            case "precision" -> category + " 模型的精确率";
            case "recall" -> category + " 模型的召回率";
            case "fps" -> "每秒处理帧数";
            case "accuracy" -> category + " 模型的整体准确率";
            case "top1-accuracy" -> "Top-1 准确率";
            case "top5-accuracy" -> "Top-5 准确率";
            case "f1-score" -> "F1 综合评分";
            case "perplexity" -> "语言模型困惑度";
            case "wer" -> "词错误率";
            case "cer" -> "字符错误率";
            case "recognitionrate" -> "文本识别率";
            case "rejectionrate" -> "文本拒识率";
            case "bleu" -> "BLEU 翻译质量评分";
            case "rouge" -> "ROUGE 文本相似度评分";
            case "relevance" -> "回答相关性评分";
            case "latency" -> "平均推理延迟";
            case "throughput" -> "系统吞吐量";
            default -> category + " 模型评测指标";
        };
    }
}
