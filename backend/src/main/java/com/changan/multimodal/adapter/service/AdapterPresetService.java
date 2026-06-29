package com.changan.multimodal.adapter.service;

import com.changan.multimodal.adapter.dto.AdapterPreset;
import com.changan.multimodal.adapter.dto.AdapterPresetRequest;
import com.changan.multimodal.common.persistence.DemoPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdapterPresetService {
    private static final String DOMAIN = "ADAPTER";
    private static final String TYPE_PRESET = "PRESET";

    private final DemoPersistenceService persistenceService;

    public List<AdapterPreset> listPresets() {
        seedIfNecessary();
        return persistenceService.findAll(DOMAIN, TYPE_PRESET, AdapterPreset.class);
    }

    public AdapterPreset findPreset(String presetId) {
        seedIfNecessary();
        return persistenceService.findOne(DOMAIN, TYPE_PRESET, presetId, AdapterPreset.class)
                .orElseThrow(() -> new IllegalArgumentException("adapter preset not found: " + presetId));
    }

    public AdapterPreset savePreset(AdapterPresetRequest request) {
        seedIfNecessary();
        String presetId = request.getPresetId() == null || request.getPresetId().isBlank()
                ? "adapter-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                : request.getPresetId();
        AdapterPreset preset = AdapterPreset.builder()
                .presetId(presetId)
                .displayName(value(request.getDisplayName(), presetId))
                .modelFamily(value(request.getModelFamily(), "CUSTOM"))
                .taskType(value(request.getTaskType(), "custom"))
                .adapterType(value(request.getAdapterType(), presetId))
                .adapterStatus(value(request.getAdapterStatus(), "DEMO"))
                .supportedModels(list(request.getSupportedModels(), List.of("custom")))
                .supportedModalities(list(request.getSupportedModalities(), List.of("image", "text", "audio")))
                .datasetFormats(list(request.getDatasetFormats(), List.of("custom-jsonl")))
                .metrics(list(request.getMetrics(), List.of("Accuracy", "Latency")))
                .requiredArtifacts(list(request.getRequiredArtifacts(), List.of("model_path", "adapter_config")))
                .inputSchema(map(request.getInputSchema()))
                .outputSchema(map(request.getOutputSchema()))
                .defaultConfig(map(request.getDefaultConfig()))
                .configurableFields(listMap(request.getConfigurableFields()))
                .runnerKind(value(request.getRunnerKind(), "LOCAL_PROCESS_JSON"))
                .runnerTemplate(value(request.getRunnerTemplate(), "python ./model-runners/demo_model_runner.py"))
                .officialDocUrls(list(request.getOfficialDocUrls(), List.of()))
                .compatibilityNotes(value(request.getCompatibilityNotes(), "Custom adapter preset."))
                .custom(request.getCustom() == null || request.getCustom())
                .updatedAt(Instant.now().toEpochMilli())
                .build();
        return persistenceService.save(DOMAIN, TYPE_PRESET, presetId, preset);
    }

    @Transactional
    public void deletePreset(String presetId) {
        seedIfNecessary();
        AdapterPreset preset = findPreset(presetId);
        if (!preset.isCustom()) {
            throw new IllegalArgumentException("Built-in adapter preset cannot be deleted: " + presetId);
        }
        persistenceService.delete(DOMAIN, TYPE_PRESET, presetId);
    }

    private void seedIfNecessary() {
        long now = Instant.now().toEpochMilli();
        defaultPresets(now).forEach(item -> {
            if (persistenceService.findOne(DOMAIN, TYPE_PRESET, item.getPresetId(), AdapterPreset.class).isEmpty()) {
                persistenceService.save(DOMAIN, TYPE_PRESET, item.getPresetId(), item);
            }
        });
    }

    private List<AdapterPreset> defaultPresets(long now) {
        return List.of(
                yolo("YOLO_DETECT", "YOLO 检测", "detect",
                        List.of("YOLOv7", "YOLOv8", "YOLO11/26"),
                        List.of("Ultralytics YOLO txt: class x_center y_center width height"),
                        Map.of("boxes", "xyxy", "scores", "confidence", "classes", "names from data.yaml"),
                        List.of("mAP", "Precision", "Recall", "IoU", "FPS"), now),
                yolo("YOLO_POSE", "YOLO 姿态", "pose",
                        List.of("YOLOv7-pose", "YOLOv8-pose", "YOLO11/26-pose"),
                        List.of("Ultralytics YOLO pose txt: class box keypoints; kpt_shape from data.yaml or output tensor"),
                        Map.of("boxes", "xyxy", "keypoints", "auto infer Nx2/Nx3", "scores", "confidence"),
                        List.of("mAP", "Precision", "Recall", "IoU", "KeypointOKS"), now),
                yolo("YOLO_SEGMENT", "YOLO 实例分割", "segment",
                        List.of("YOLOv8-seg", "YOLO11/26-seg"),
                        List.of("Ultralytics YOLO segment txt: class polygon points, variable row length"),
                        Map.of("masks", "polygons or bitmap masks", "boxes", "optional xyxy", "scores", "confidence"),
                        List.of("Mask-mAP", "Precision", "Recall", "IoU"), now),
                yolo("YOLO_CLASSIFY", "YOLO 图像分类", "classify",
                        List.of("YOLOv8-cls", "YOLO11/26-cls"),
                        List.of("ImageFolder: class-name/image-file"),
                        Map.of("label", "class name", "score", "confidence", "topk", "ranked classes"),
                        List.of("Top1-Accuracy", "Top5-Accuracy", "Latency"), now),
                yolo("YOLO_OBB", "YOLO 旋转框", "obb",
                        List.of("YOLOv8-obb", "YOLO11/26-obb"),
                        List.of("Ultralytics YOLO OBB txt: class x1 y1 x2 y2 x3 y3 x4 y4"),
                        Map.of("obb", "four corner points", "xywhr", "optional internal representation"),
                        List.of("mAP", "Precision", "Recall", "Rotated-IoU"), now),
                preset("PADDLEOCR_DB_DET", "DBNet 文本检测", "OCR", "text-detection", "PADDLEOCR_DB_DET",
                        List.of("DBNet", "PP-OCR det"), List.of("image"),
                        List.of("PaddleOCR det dataset", "ICDAR polygon labels", "image directory"),
                        List.of("Detection-Hmean", "Precision", "Recall"),
                        List.of("model_dir or model_name", "engine", "device"),
                        Map.of("input", "image path/list/ndarray", "engine", "paddle_static|onnxruntime|transformers"),
                        Map.of("dt_polys", "N text polygons, four vertices", "dt_scores", "confidence list"),
                        Map.of("engine", "paddle_static", "limit_side_len", 960, "box_thresh", 0.6, "unclip_ratio", 1.5),
                        fields("engine", "device", "limit_side_len", "box_thresh", "unclip_ratio"),
                        "PYTHON_PADDLEOCR", "python ./model-runners/paddleocr_adapter.py --task text_detection",
                        List.of("https://www.paddleocr.ai/latest/en/version3.x/module_usage/text_detection.html"),
                        "Detection boxes are polygons. Metrics should compare polygons or their quadrilateral boxes.", false, now),
                preset("PADDLEOCR_CRNN_REC", "CRNN 文本识别", "OCR", "text-recognition", "PADDLEOCR_CRNN_REC",
                        List.of("CRNN", "PP-OCR rec"), List.of("image"),
                        List.of("PaddleOCR rec label file: image_path<TAB>text", "cropped text-line image directory"),
                        List.of("RecognitionRate", "RejectionRate", "Accuracy", "CER"),
                        List.of("model_dir or model_name", "character_dict_path optional", "engine"),
                        Map.of("input", "cropped line image path/list/ndarray"),
                        Map.of("rec_text", "recognized text", "rec_score", "confidence"),
                        Map.of("engine", "paddle_static", "input_shape", "(3,48,320)", "reject_threshold", 0.5),
                        fields("engine", "device", "input_shape", "character_dict_path", "reject_threshold"),
                        "PYTHON_PADDLEOCR", "python ./model-runners/paddleocr_adapter.py --task text_recognition",
                        List.of("https://www.paddleocr.ai/latest/en/version3.x/module_usage/text_recognition.html"),
                        "Recognition metrics require ground-truth text. Low rec_score can be counted as rejection.", false, now),
                preset("TORCHVISION_CLASSIFY", "MobileNet/ResNet50 分类", "VISION", "image-classification", "TORCHVISION_CLASSIFY",
                        List.of("MobileNet", "ResNet50"), List.of("image"),
                        List.of("ImageFolder", "CSV/JSONL image,label"),
                        List.of("Top1-Accuracy", "Top5-Accuracy", "Latency"),
                        List.of("weights or model_path", "class_names"),
                        Map.of("input", "image path/list", "preprocess", "resize/crop/normalize from selected weights"),
                        Map.of("label", "class", "score", "confidence", "top5", "ranked predictions"),
                        Map.of("image_size", 224, "topk", 5, "weights", "DEFAULT"),
                        fields("image_size", "topk", "weights", "class_names_path"),
                        "PYTHON_TORCHVISION", "python ./model-runners/torchvision_classify_adapter.py",
                        List.of("https://docs.pytorch.org/vision/stable/models.html"),
                        "Preset follows torchvision weights transforms; custom models need matching class list.", false, now),
                preset("TRANSFORMERS_NLP", "BERT/LSTM/GRU/Transformer/RoBERTa 语义", "NLP", "text-classification", "TRANSFORMERS_NLP",
                        List.of("BERT", "RoBERTa", "Transformer", "LSTM", "GRU"), List.of("text"),
                        List.of("CSV/JSONL text,label", "sentence-pair CSV/JSONL"),
                        List.of("Accuracy", "F1-Score", "Precision", "Recall"),
                        List.of("model_path or model_id", "tokenizer or vocab", "label_map"),
                        Map.of("input", "text or text_pair", "max_length", "tokenizer/vocab padding truncation"),
                        Map.of("label", "predicted class", "score", "confidence", "logits", "optional"),
                        Map.of("task", "text-classification", "max_length", 512, "batch_size", 8),
                        fields("task", "max_length", "batch_size", "label_map", "tokenizer_path"),
                        "PYTHON_TRANSFORMERS", "python ./model-runners/transformers_nlp_adapter.py",
                        List.of("https://huggingface.co/docs/transformers/en/main_classes/pipelines"),
                        "Transformer models can use Hugging Face pipeline; LSTM/GRU require vocab and local script.", false, now),
                preset("ASR_SPEECH_TO_TEXT", "DeepSpeech/Whisper 转写", "ASR", "automatic-speech-recognition", "ASR_SPEECH_TO_TEXT",
                        List.of("DeepSpeech", "Whisper"), List.of("audio"),
                        List.of("WAV/MP3/FLAC + transcript CSV/JSONL"),
                        List.of("WER", "CER", "Latency"),
                        List.of("model_path or model_id", "sample_rate", "language optional"),
                        Map.of("input", "audio path/list", "preprocess", "decode/resample/mono"),
                        Map.of("text", "transcript", "segments", "optional timestamps", "score", "optional confidence"),
                        Map.of("sample_rate", 16000, "language", "auto", "task", "transcribe"),
                        fields("sample_rate", "language", "task", "batch_size"),
                        "PYTHON_ASR", "python ./model-runners/asr_adapter.py",
                        List.of("https://github.com/openai/whisper", "https://github.com/mozilla/DeepSpeech"),
                        "WER/CER require reference transcript. Whisper and DeepSpeech have different model artifacts.", false, now),
                preset("VLM_CHAT", "Qwen-VL/Yi-VL/VisualGLM 多模态", "VLM", "image-text-to-text", "VLM_CHAT",
                        List.of("Qwen-VL", "Yi-VL", "VisualGLM"), List.of("image", "text"),
                        List.of("JSONL image,prompt,answer", "single image + prompt"),
                        List.of("BLEU", "ROUGE", "Relevance", "Latency"),
                        List.of("model_id/path", "processor/tokenizer", "prompt_template"),
                        Map.of("input", "image plus prompt", "switch_key", "modelId for dynamic switching"),
                        Map.of("answer", "generated text", "modelId", "active model", "usage", "optional tokens"),
                        Map.of("temperature", 0.2, "max_new_tokens", 256, "prompt_template", "{question}"),
                        fields("temperature", "max_new_tokens", "prompt_template", "device_map"),
                        "PYTHON_TRANSFORMERS_VLM", "python ./model-runners/vlm_adapter.py",
                        List.of("https://huggingface.co/docs/transformers/en/main_classes/pipelines"),
                        "Dynamic switching is implemented by selecting another registered model with the same input sample.", false, now),
                preset("CUSTOM_JSON_PROCESS", "甲方自研标准 JSON 进程", "CUSTOM", "custom-json-process", "CUSTOM_JSON_PROCESS",
                        List.of("甲方自研模型", "微调改进模型"), List.of("image", "text", "audio", "sensor"),
                        List.of("JSON/JSONL plus artifact references"),
                        List.of("Accuracy", "Latency", "CustomMetric"),
                        List.of("runtimeCommand", "adapter_config"),
                        Map.of("stdin", "InferenceRequest JSON"),
                        Map.of("stdout", "{outputs, metrics, durationMs, runnerStatus}"),
                        Map.of("timeoutMs", 30000, "strictJson", true),
                        fields("timeoutMs", "strictJson", "metric_mapping", "output_mapping"),
                        "LOCAL_PROCESS_JSON", "python ./model-runners/custom_json_adapter.py",
                        List.of(),
                        "Standard process adapter for proprietary or fine-tuned models.", false, now)
        );
    }

    private AdapterPreset yolo(String id, String name, String task, List<String> models, List<String> datasetFormats,
                               Map<String, Object> outputSchema, List<String> metrics, long now) {
        return preset(id, name, "YOLO", task, id, models, List.of("image", "video"), datasetFormats, metrics,
                List.of("model_path", "data.yaml optional", "class names optional"),
                Map.of("input", "image/video path list", "imgsz", "auto or configured", "task", task),
                outputSchema,
                Map.of("conf", 0.25, "iou", 0.45, "max_det", 300, "imgsz", "auto", "kpt_shape", "auto"),
                fields("conf", "iou", "max_det", "imgsz", "device", "data_yaml"),
                "PYTHON_ONNX_OR_ULTRALYTICS", "python ./model-runners/yolo_adapter.py --task " + task,
                List.of("https://docs.ultralytics.com/datasets/detect/",
                        "https://docs.ultralytics.com/datasets/pose/",
                        "https://docs.ultralytics.com/datasets/segment/",
                        "https://docs.ultralytics.com/datasets/obb/"),
                "YOLO task preset. Pose keypoint count is inferred from data.yaml kpt_shape or model output tensor.", false, now);
    }

    private AdapterPreset preset(String id, String displayName, String family, String task, String adapterType,
                                 List<String> models, List<String> modalities, List<String> formats,
                                 List<String> metrics, List<String> artifacts, Map<String, Object> inputSchema,
                                 Map<String, Object> outputSchema, Map<String, Object> defaultConfig,
                                 List<Map<String, Object>> fields, String runnerKind, String runnerTemplate,
                                 List<String> docUrls, String notes, boolean custom, long now) {
        return AdapterPreset.builder()
                .presetId(id)
                .displayName(displayName)
                .modelFamily(family)
                .taskType(task)
                .adapterType(adapterType)
                .adapterStatus("DEMO")
                .supportedModels(models)
                .supportedModalities(modalities)
                .datasetFormats(formats)
                .metrics(metrics)
                .requiredArtifacts(artifacts)
                .inputSchema(inputSchema)
                .outputSchema(outputSchema)
                .defaultConfig(defaultConfig)
                .configurableFields(fields)
                .runnerKind(runnerKind)
                .runnerTemplate(runnerTemplate)
                .officialDocUrls(docUrls)
                .compatibilityNotes(notes)
                .custom(custom)
                .updatedAt(now)
                .build();
    }

    private List<Map<String, Object>> fields(String... names) {
        return java.util.Arrays.stream(names)
                .map(name -> Map.<String, Object>of("name", name, "label", name, "editable", true))
                .toList();
    }

    private String value(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private <T> List<T> list(List<T> value, List<T> fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private List<Map<String, Object>> listMap(List<Map<String, Object>> value) {
        return value == null ? List.of() : value;
    }

    private Map<String, Object> map(Map<String, Object> value) {
        return value == null ? new LinkedHashMap<>() : value;
    }
}
