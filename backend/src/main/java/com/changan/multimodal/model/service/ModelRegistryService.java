package com.changan.multimodal.model.service;

import com.changan.multimodal.adapter.dto.AdapterPreset;
import com.changan.multimodal.adapter.service.AdapterPresetService;
import com.changan.multimodal.common.persistence.DemoPersistenceService;
import com.changan.multimodal.model.dto.ModelDescriptor;
import com.changan.multimodal.model.dto.ModelLifecycleRequest;
import com.changan.multimodal.model.dto.ModelRunLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModelRegistryService {

    private static final String DOMAIN = "MODEL";
    private static final String TYPE_DESCRIPTOR = "DESCRIPTOR";
    private static final String TYPE_RUN_LOG = "RUN_LOG";

    private final DemoPersistenceService persistenceService;
    private final AdapterPresetService adapterPresetService;

    public List<ModelDescriptor> listModels() {
        seedIfNecessary();
        return persistenceService.findAll(DOMAIN, TYPE_DESCRIPTOR, ModelDescriptor.class);
    }

    public ModelDescriptor register(ModelLifecycleRequest request) {
        long now = Instant.now().toEpochMilli();
        String modelId = request.getModelId() == null || request.getModelId().isBlank()
                ? "model-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                : request.getModelId();
        String category = request.getModelCategory() == null ? "CUSTOM" : request.getModelCategory();
        AdapterPreset preset = resolvePreset(request.getAdapterType(), category);
        ModelDescriptor descriptor = ModelDescriptor.builder()
                .modelId(modelId)
                .modelName(request.getModelName() == null ? modelId : request.getModelName())
                .version(request.getVersion() == null ? "1.0.0" : request.getVersion())
                .activeVersion(request.getVersion() == null ? "1.0.0" : request.getVersion())
                .algorithmType(request.getAlgorithmType() == null ? preset.getTaskType() : request.getAlgorithmType())
                .deploymentStatus("OFFLINE")
                .supportedModalities(request.getSupportedModalities() == null ? preset.getSupportedModalities() : request.getSupportedModalities())
                .todo("Registered with adapterType=" + preset.getAdapterType() + ". Runtime uses standardized JSON input/output.")
                .invocationCount(0)
                .averageLatency(0.0)
                .modelCategory(category)
                .availableMetrics(request.getAvailableMetrics() == null ? preset.getMetrics() : request.getAvailableMetrics())
                .isCustom(true)
                .runtimeCommand(request.getRuntimeCommand())
                .packageUri(request.getPackageUri())
                .adapterType(preset.getAdapterType())
                .adapterStatus(request.getAdapterStatus() == null ? inferAdapterStatus(request, preset) : request.getAdapterStatus())
                .adapterConfig(request.getAdapterConfig() == null ? preset.getDefaultConfig() : request.getAdapterConfig())
                .datasetFormats(request.getDatasetFormats() == null ? preset.getDatasetFormats() : request.getDatasetFormats())
                .outputSchema(preset.getOutputSchema())
                .updatedAt(now)
                .build();
        return persistenceService.save(DOMAIN, TYPE_DESCRIPTOR, modelId, descriptor);
    }

    public ModelDescriptor analyzeModel(String onnxFileName, long fileSize) {
        ModelLifecycleRequest request = new ModelLifecycleRequest();
        request.setModelId("onnx-" + System.currentTimeMillis());
        request.setModelName(onnxFileName == null ? "onnx-model" : onnxFileName.replace(".onnx", ""));
        request.setVersion("1.0.0");
        request.setAlgorithmType("onnx-inferred");
        request.setModelCategory("CUSTOM");
        request.setSupportedModalities(List.of("image", "tensor"));
        request.setAvailableMetrics(List.of("Latency", "Throughput"));
        request.setPackageUri("upload://" + onnxFileName + "?size=" + fileSize);
        request.setAdapterType("CUSTOM_JSON_PROCESS");
        ModelDescriptor analyzed = register(request).toBuilder()
                .deploymentStatus("ANALYZED")
                .todo("ONNX metadata registered. Select YOLO/PaddleOCR/TorchVision/etc adapter before strict evaluation.")
                .build();
        return persistenceService.save(DOMAIN, TYPE_DESCRIPTOR, analyzed.getModelId(), analyzed);
    }

    public ModelDescriptor changeStatus(String modelId, String status) {
        ModelDescriptor current = findById(modelId);
        ModelDescriptor updated = current.toBuilder()
                .deploymentStatus(status)
                .updatedAt(Instant.now().toEpochMilli())
                .build();
        return persistenceService.save(DOMAIN, TYPE_DESCRIPTOR, modelId, updated);
    }

    public ModelDescriptor rollback(String modelId, String version) {
        ModelDescriptor current = findById(modelId);
        ModelDescriptor updated = current.toBuilder()
                .version(version)
                .activeVersion(version)
                .deploymentStatus("RUNNING")
                .todo("Rolled back to version " + version)
                .updatedAt(Instant.now().toEpochMilli())
                .build();
        return persistenceService.save(DOMAIN, TYPE_DESCRIPTOR, modelId, updated);
    }

    public ModelDescriptor findById(String modelId) {
        seedIfNecessary();
        return persistenceService.findOne(DOMAIN, TYPE_DESCRIPTOR, modelId, ModelDescriptor.class)
                .orElseThrow(() -> new IllegalArgumentException("Model not found: " + modelId));
    }

    public ModelRunLog appendRunLog(ModelRunLog log) {
        return persistenceService.save(DOMAIN, TYPE_RUN_LOG, log.getLogId(), log);
    }

    public List<ModelRunLog> listRunLogs() {
        return persistenceService.findAll(DOMAIN, TYPE_RUN_LOG, ModelRunLog.class);
    }

    private void seedIfNecessary() {
        if (persistenceService.exists(DOMAIN, TYPE_DESCRIPTOR)) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        for (ModelDescriptor descriptor : defaultModels(now)) {
            persistenceService.save(DOMAIN, TYPE_DESCRIPTOR, descriptor.getModelId(), descriptor);
        }
    }

    private List<ModelDescriptor> defaultModels(long now) {
        return List.of(
                descriptor("yolov7-detection", "YOLOv7 Detect", "1.0.0", "object-detection", "OBJECT_DETECTION", "YOLO_DETECT", now),
                descriptor("yolov8-detection", "YOLOv8 Detect", "8.2.0", "object-detection", "OBJECT_DETECTION", "YOLO_DETECT", now),
                descriptor("yolov8-pose", "YOLOv8 Pose", "8.2.0", "pose-estimation", "OBJECT_DETECTION", "YOLO_POSE", now),
                descriptor("yolov8-segment", "YOLOv8 Segment", "8.2.0", "instance-segmentation", "OBJECT_DETECTION", "YOLO_SEGMENT", now),
                descriptor("dbnet-detector", "DBNet", "2.0", "text-detection", "OCR", "PADDLEOCR_DB_DET", now),
                descriptor("crnn-recognizer", "CRNN", "1.1", "text-recognition", "OCR", "PADDLEOCR_CRNN_REC", now),
                descriptor("mobilenet-classifier", "MobileNet", "v3-large", "image-classification", "IMAGE_CLASSIFICATION", "TORCHVISION_CLASSIFY", now),
                descriptor("resnet50-classifier", "ResNet50", "v1.5", "image-classification", "IMAGE_CLASSIFICATION", "TORCHVISION_CLASSIFY", now),
                descriptor("bert-base", "BERT", "base-chinese", "semantic-analysis", "SEMANTIC_ANALYSIS", "TRANSFORMERS_NLP", now),
                descriptor("lstm-sentiment", "LSTM", "1.2", "sentiment-analysis", "SEMANTIC_ANALYSIS", "TRANSFORMERS_NLP", now),
                descriptor("gru-classifier", "GRU", "1.0", "text-classification", "SEMANTIC_ANALYSIS", "TRANSFORMERS_NLP", now),
                descriptor("transformer-base", "Transformer", "base", "semantic-analysis", "SEMANTIC_ANALYSIS", "TRANSFORMERS_NLP", now),
                descriptor("roberta-chinese", "RoBERTa", "chinese-wwm", "semantic-analysis", "SEMANTIC_ANALYSIS", "TRANSFORMERS_NLP", now),
                descriptor("deepspeech2", "DeepSpeech", "2.0", "speech-to-text", "SPEECH_RECOGNITION", "ASR_SPEECH_TO_TEXT", now),
                descriptor("whisper-demo", "Whisper", "large-v3", "speech-to-text", "SPEECH_RECOGNITION", "ASR_SPEECH_TO_TEXT", now),
                descriptor("qwen-vl", "Qwen-VL", "1.5", "vision-language", "VISION_LANGUAGE", "VLM_CHAT", now),
                descriptor("yi-vl", "Yi-VL", "6B", "vision-language", "VISION_LANGUAGE", "VLM_CHAT", now),
                descriptor("visualglm", "VisualGLM", "6B", "vision-language", "VISION_LANGUAGE", "VLM_CHAT", now),
                descriptor("custom-model-v1", "Party-A Custom Model", "1.0.0", "custom", "CUSTOM", "CUSTOM_JSON_PROCESS", now),
                descriptor("custom-finetuned", "Fine-tuned Improved Model", "2.1.0", "custom-finetuned", "CUSTOM", "CUSTOM_JSON_PROCESS", now)
        );
    }

    private ModelDescriptor descriptor(String id, String name, String version, String algorithmType, String category,
                                       String adapterType, long now) {
        AdapterPreset preset = resolvePreset(adapterType, category);
        return ModelDescriptor.builder()
                .modelId(id)
                .modelName(name)
                .version(version)
                .activeVersion(version)
                .algorithmType(algorithmType)
                .deploymentStatus("RUNNING")
                .supportedModalities(preset.getSupportedModalities())
                .todo("Built-in adapter preset: " + preset.getDisplayName() + ". Real weights require runtimeCommand/packageUri.")
                .invocationCount(0)
                .averageLatency(0.0)
                .modelCategory(category)
                .availableMetrics(preset.getMetrics())
                .isCustom(category.equals("CUSTOM"))
                .runtimeCommand(null)
                .packageUri("demo://local/" + id)
                .adapterType(preset.getAdapterType())
                .adapterStatus("DEMO")
                .adapterConfig(preset.getDefaultConfig())
                .datasetFormats(preset.getDatasetFormats())
                .outputSchema(preset.getOutputSchema())
                .updatedAt(now)
                .build();
    }

    private AdapterPreset resolvePreset(String adapterType, String modelCategory) {
        String target = adapterType;
        if (target == null || target.isBlank()) {
            target = switch (modelCategory == null ? "" : modelCategory) {
                case "OBJECT_DETECTION" -> "YOLO_DETECT";
                case "OCR" -> "PADDLEOCR_DB_DET";
                case "IMAGE_CLASSIFICATION" -> "TORCHVISION_CLASSIFY";
                case "SEMANTIC_ANALYSIS" -> "TRANSFORMERS_NLP";
                case "SPEECH_RECOGNITION" -> "ASR_SPEECH_TO_TEXT";
                case "VISION_LANGUAGE" -> "VLM_CHAT";
                default -> "CUSTOM_JSON_PROCESS";
            };
        }
        final String resolved = target;
        return adapterPresetService.listPresets().stream()
                .filter(item -> item.getAdapterType().equals(resolved) || item.getPresetId().equals(resolved))
                .findFirst()
                .orElseGet(() -> adapterPresetService.findPreset("CUSTOM_JSON_PROCESS"));
    }

    private String inferAdapterStatus(ModelLifecycleRequest request, AdapterPreset preset) {
        if (request.getRuntimeCommand() != null && !request.getRuntimeCommand().isBlank()) {
            return "REAL";
        }
        return preset.getAdapterStatus();
    }
}
