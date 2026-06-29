package com.changan.multimodal.data.service;

import com.changan.multimodal.common.persistence.DemoPersistenceService;
import com.changan.multimodal.data.dto.*;
import com.changan.multimodal.data.persistence.DatasetRecord;
import com.changan.multimodal.data.persistence.DatasetRecordRepository;
import com.changan.multimodal.data.persistence.SampleRecord;
import com.changan.multimodal.data.persistence.SampleRecordRepository;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataPipelineService {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "bmp", "gif", "webp");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "json", "xml", "csv", "yaml", "yml");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("wav", "mp3", "flac");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("avi", "mp4", "mov");
    private static final Set<String> LABEL_EXTENSIONS = TEXT_EXTENSIONS;
    private static final Set<String> PAIRING_DIRECTORY_NAMES = Set.of(
            "image", "images", "img", "label", "labels", "annotation", "annotations"
    );
    private static final String DOMAIN = "DATA";
    private static final String TYPE_DATASET = "DATASET";
    private static final String TYPE_PIPELINE = "PIPELINE";
    private static final String TYPE_SAMPLE = "SAMPLE";
    private static final String TYPE_DATASOURCE = "DATASOURCE";
    private static final String TYPE_RESULT = "RESULT";

    private final Map<String, DataIngestResponse> datasetStore = new ConcurrentHashMap<>();
    private final Map<String, DataSource> dataSourceStore = new ConcurrentHashMap<>();
    private final WsMessageRouter wsMessageRouter;
    private final DemoPersistenceService persistenceService;
    private final DatasetRecordRepository datasetRecordRepository;
    private final SampleRecordRepository sampleRecordRepository;
    private final ManagedSampleStorageService sampleStorageService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DataIngestResponse registerDataset(DataIngestRequest request) {
        String datasetId = UUID.randomUUID().toString().replace("-", "");
        Set<String> modalities = new LinkedHashSet<>();
        for (DataAsset asset : request.getAssets()) {
            modalities.add(asset.getModality() == null ? inferModality(asset.getUri()) : asset.getModality());
        }
        DataIngestResponse response = DataIngestResponse.builder()
                .datasetId(datasetId)
                .datasetName(request.getDatasetName())
                .assetCount(request.getAssets().size())
                .supportedModalities(List.copyOf(modalities))
                .version("v1")
                .status("REGISTERED")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        saveDatasetRecord(response);
        datasetStore.put(datasetId, response);
        persistenceService.save(DOMAIN, TYPE_DATASET, datasetId, response);
        publishCatalogChanged("DATASET_REGISTERED", datasetId, 0);
        return response;
    }

    @Transactional
    public DataIngestResponse uploadDataset(String datasetName, List<String> tags,
                                            List<String> relativePaths, List<MultipartFile> files) {
        if (datasetName == null || datasetName.isBlank()) {
            throw new IllegalArgumentException("datasetName不能为空");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files不能为空");
        }

        List<UploadFile> uploads = normalizeUploads(files, relativePaths);
        UploadPlan plan = planUpload(uploads);
        String datasetId = UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().toEpochMilli();
        List<ManagedSampleStorageService.StoredSample> storedFiles = new ArrayList<>();
        Set<String> modalities = new LinkedHashSet<>();
        int savedCount = 0;

        try {
            for (SamplePair pair : plan.pairs()) {
                ManagedSampleStorageService.StoredSample image = sampleStorageService.store(pair.image().file());
                storedFiles.add(image);
                ManagedSampleStorageService.StoredSample label = sampleStorageService.store(pair.label().file());
                storedFiles.add(label);
                SampleRecord record = baseRecord(datasetId, image, image.originalName(), "image", now);
                record.setImageRelativePath(pair.image().relativePath());
                record.setLabelStorageKey(label.storageKey());
                record.setLabelRelativePath(pair.label().relativePath());
                record.setLabelOriginalName(label.originalName());
                record.setLabelContentType(firstNonBlank(label.contentType(), contentTypeFor(label.originalName())));
                record.setLabelFileSize(label.fileSize());
                record.setLabelSha256(label.sha256());
                record.setTagsJson(writeJson(tags == null ? List.of() : tags));
                record.setMetadataJson(writeJson(Map.of(
                        "uploadSource", "browser-folder",
                        "pairKey", pair.pairKey(),
                        "imageRelativePath", pair.image().relativePath(),
                        "labelRelativePath", pair.label().relativePath(),
                        "eventType", eventTypeFromPath(pair.image().relativePath()),
                        "timestamp", now
                )));
                sampleRecordRepository.save(record);
                modalities.add("image");
                savedCount++;
            }

            for (UploadFile upload : plan.standalone()) {
                ManagedSampleStorageService.StoredSample stored = sampleStorageService.store(upload.file());
                storedFiles.add(stored);
                String modality = inferModality(upload.relativePath());
                SampleRecord record = baseRecord(datasetId, stored, stored.originalName(), modality, now);
                record.setImageRelativePath(upload.relativePath());
                record.setContentType(firstNonBlank(stored.contentType(), contentTypeFor(stored.originalName())));
                record.setTagsJson(writeJson(tags == null ? List.of() : tags));
                record.setMetadataJson(writeJson(Map.of(
                        "uploadSource", "browser-folder",
                        "relativePath", upload.relativePath(),
                        "eventType", eventTypeFromPath(upload.relativePath()),
                        "timestamp", now
                )));
                sampleRecordRepository.save(record);
                modalities.add(modality);
                savedCount++;
            }

            DataIngestResponse response = DataIngestResponse.builder()
                    .datasetId(datasetId)
                    .datasetName(datasetName.trim())
                    .assetCount(savedCount)
                    .supportedModalities(List.copyOf(modalities))
                    .version("v1")
                    .status("READY")
                    .createdAt(Instant.ofEpochMilli(now))
                    .updatedAt(Instant.ofEpochMilli(now))
                    .build();
            saveDatasetRecord(response);
            datasetStore.put(datasetId, response);
            persistenceService.save(DOMAIN, TYPE_DATASET, datasetId, response);
            publishCatalogChanged("DATASET_UPLOADED", datasetId, savedCount);
            return response;
        } catch (RuntimeException ex) {
            storedFiles.forEach(stored -> sampleStorageService.deleteQuietly(stored.storageKey()));
            throw ex;
        }
    }

    public List<DataIngestResponse> listDatasets() {
        Map<String, DataIngestResponse> result = new LinkedHashMap<>();
        datasetRecordRepository.findAllByOrderByUpdatedAtDesc()
                .forEach(record -> result.put(record.getDatasetId(), toDataset(record)));
        persistenceService.findAll(DOMAIN, TYPE_DATASET, DataIngestResponse.class)
                .forEach(item -> result.putIfAbsent(item.getDatasetId(), item));
        return new ArrayList<>(result.values());
    }

    public List<Sample> listDatasetSamples(String datasetId) {
        requireDataset(datasetId);
        return sampleRecordRepository.findByDatasetIdOrderByCreatedAtDesc(datasetId).stream()
                .map(this::toSample)
                .toList();
    }

    public Sample getSample(String sampleId) {
        return sampleRecordRepository.findById(sampleId)
                .map(this::toSample)
                .orElseThrow(() -> new IllegalArgumentException("样本不存在: " + sampleId));
    }

    public Path resolveSamplePath(String sampleId) {
        SampleRecord sample = requireSample(sampleId);
        return sampleStorageService.requireExisting(sample.getStorageKey());
    }

    public Optional<Path> resolveSampleLabelPath(String sampleId) {
        SampleRecord sample = requireSample(sampleId);
        if (sample.getLabelStorageKey() == null || sample.getLabelStorageKey().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(sampleStorageService.requireExisting(sample.getLabelStorageKey()));
    }

    public Path requireSampleLabelPath(String sampleId) {
        return resolveSampleLabelPath(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("样本没有对应标签: " + sampleId));
    }

    public String sampleContentType(String sampleId) {
        return requireSample(sampleId).getContentType();
    }

    public String sampleOriginalName(String sampleId) {
        return requireSample(sampleId).getOriginalName();
    }

    public String sampleLabelContentType(String sampleId) {
        return requireSample(sampleId).getLabelContentType();
    }

    public String sampleLabelOriginalName(String sampleId) {
        return requireSample(sampleId).getLabelOriginalName();
    }

    public DataPipelineResponse runPipeline(DataPipelineRequest request) {
        DataIngestResponse dataset = requireDataset(request.getDatasetId());
        String pipelineId = UUID.randomUUID().toString().replace("-", "");
        List<String> executedOperations = request.getOperations().stream()
                .map(DataPipelineOperation::getOperation)
                .toList();
        List<String> generatedAssets = executedOperations.stream()
                .map(operation -> request.getDatasetId() + ":" + operation + ":artifact")
                .toList();
        DataPipelineResponse response = DataPipelineResponse.builder()
                .pipelineId(pipelineId)
                .datasetId(request.getDatasetId())
                .executedOperations(executedOperations)
                .generatedAssets(generatedAssets)
                .status("COMPLETED")
                .build();
        wsMessageRouter.broadcast(WsMessageType.DATA_PIPELINE, Map.of(
                "pipelineId", pipelineId,
                "datasetId", request.getDatasetId(),
                "datasetName", dataset.getDatasetName(),
                "operations", executedOperations,
                "status", "COMPLETED"
        ));
        return persistenceService.save(DOMAIN, TYPE_PIPELINE, pipelineId, response);
    }

    public List<DataSource> listDataSources() {
        Map<String, DataSource> merged = new LinkedHashMap<>();
        persistenceService.findAll(DOMAIN, TYPE_DATASOURCE, DataSource.class).forEach(item -> merged.put(item.getSourceId(), item));
        dataSourceStore.values().forEach(item -> merged.put(item.getSourceId(), item));
        return new ArrayList<>(merged.values());
    }

    public DataSource addDataSource(DataSource dataSource) {
        dataSource.setSourceId(UUID.randomUUID().toString().replace("-", ""));
        dataSource.setConnectedAt(Instant.now());
        dataSource.setStatus("CONNECTED");
        dataSourceStore.put(dataSource.getSourceId(), dataSource);
        return persistenceService.save(DOMAIN, TYPE_DATASOURCE, dataSource.getSourceId(), dataSource);
    }

    public List<Sample> searchSamples(SampleSearchRequest request) {
        Map<String, Sample> merged = new LinkedHashMap<>();
        sampleRecordRepository.findAllByOrderByCreatedAtDesc()
                .forEach(item -> merged.put(item.getSampleId(), toSample(item)));
        persistenceService.findAll(DOMAIN, TYPE_SAMPLE, Sample.class).forEach(item -> merged.put(item.getSampleId(), item));
        return merged.values().stream()
                .filter(sample -> request.getDatasetId() == null || request.getDatasetId().isBlank() ||
                        request.getDatasetId().equals(sample.getDatasetId()))
                .filter(sample -> request.getKeyword() == null || request.getKeyword().isBlank() || sampleMatchesKeyword(sample, request.getKeyword()))
                .filter(sample -> request.getDataType() == null || request.getDataType().isBlank() ||
                        sample.getDataType().equals(request.getDataType()))
                .filter(sample -> request.getEventType() == null || request.getEventType().isBlank() ||
                        (sample.getMetadata() != null && request.getEventType().equals(String.valueOf(sample.getMetadata().get("eventType")))))
                .filter(sample -> request.getStartTime() == null || sample.getCreatedAt() == null || !sample.getCreatedAt().isBefore(request.getStartTime()))
                .filter(sample -> request.getEndTime() == null || sample.getCreatedAt() == null || !sample.getCreatedAt().isAfter(request.getEndTime()))
                .filter(sample -> request.getTags() == null || request.getTags().isEmpty() ||
                        (sample.getTags() != null && sample.getTags().stream().anyMatch(tag -> request.getTags().contains(tag))))
                .collect(Collectors.toList());
    }

    @Transactional
    public DataProcessingResponse processData(DataProcessingRequest request) {
        List<String> sourceIds = processingSourceIds(request);
        List<ProcessingStep> steps = request.getSteps() == null || request.getSteps().isEmpty()
                ? List.of(defaultStep("cleaning"))
                : request.getSteps();
        List<String> executed = new ArrayList<>();
        List<String> processedIds = new ArrayList<>();
        SampleRecord source = null;
        for (String sourceId : sourceIds) {
            source = requireSample(sourceId);
            ProcessingPayload payload = readPayload(source);
            List<String> sampleExecuted = new ArrayList<>();
            for (ProcessingStep step : steps) {
                payload = applyProcessingStep(payload, source, step);
                sampleExecuted.add(step.getType());
            }
            if (executed.isEmpty()) {
                executed.addAll(sampleExecuted);
            }
            String suffix = sampleExecuted.isEmpty() ? "processed" : String.join("-", sampleExecuted);
            String outputName = derivedName(source.getOriginalName(), suffix, payload.extension());
            Map<String, Object> metadata = derivedMetadata(source, "processing");
            metadata.put("steps", sampleExecuted);
            String sampleId = saveDerivedSample(source.getDatasetId(), payload, outputName, source.getDataType(),
                    mergeTags(source, List.of("processed")), metadata);
            processedIds.add(sampleId);
        }

        String taskId = UUID.randomUUID().toString().replace("-", "");
        DataProcessingResponse response = DataProcessingResponse.builder()
                .taskId(taskId)
                .status("COMPLETED")
                .processedSampleIds(processedIds)
                .results(Map.of(
                        "taskId", taskId,
                        "originalSampleIds", sourceIds,
                        "executedSteps", executed,
                        "generatedSampleIds", processedIds
                ))
                .build();
        persistenceService.save(DOMAIN, TYPE_RESULT, taskId, response);
        publishCatalogChanged("DATA_PROCESSED", source.getDatasetId(), processedIds.size());
        return response;
    }

    @Transactional
    public List<String> augmentData(DataAugmentationRequest request) {
        if (request.getSampleIds() == null || request.getSampleIds().isEmpty()) {
            throw new IllegalArgumentException("sampleIds不能为空");
        }
        int factor = Math.max(1, request.getAugmentationFactor());
        List<AugmentationConfig> configs = request.getConfigs() == null || request.getConfigs().isEmpty()
                ? defaultAugmentations(request.getDataType())
                : request.getConfigs();
        List<String> augmentedIds = new ArrayList<>();
        for (String sampleId : request.getSampleIds()) {
            SampleRecord source = requireSample(sampleId);
            for (int round = 0; round < factor; round++) {
                for (AugmentationConfig config : configs) {
                    ProcessingPayload payload = applyAugmentation(readPayload(source), source, config, round);
                    String method = config.getMethod() == null ? "augment" : config.getMethod();
                    String outputName = derivedName(source.getOriginalName(), method + "-" + (round + 1), payload.extension());
                    Map<String, Object> metadata = derivedMetadata(source, "augmentation");
                    metadata.put("method", method);
                    metadata.put("round", round + 1);
                    String newId = saveDerivedSample(source.getDatasetId(), payload, outputName, source.getDataType(),
                            mergeTags(source, List.of("augmented", method)), metadata);
                    augmentedIds.add(newId);
                }
            }
        }
        persistenceService.save(DOMAIN, TYPE_RESULT, "augmentation-" + UUID.randomUUID().toString().replace("-", ""), augmentedIds);
        if (!augmentedIds.isEmpty()) {
            publishCatalogChanged("DATA_AUGMENTED", requireSample(request.getSampleIds().get(0)).getDatasetId(), augmentedIds.size());
        }
        return augmentedIds;
    }

    @Transactional
    public List<String> fuseData(DataFusionRequest request) {
        if (request.getSampleIds() == null || request.getSampleIds().size() < 2) {
            throw new IllegalArgumentException("融合至少需要2个样本");
        }
        List<SampleRecord> sources = request.getSampleIds().stream().map(this::requireSample).toList();
        String datasetId = sources.get(0).getDatasetId();
        List<Map<String, Object>> components = new ArrayList<>();
        for (SampleRecord source : sources) {
            ProcessingPayload payload = readPayload(source);
            components.add(Map.of(
                    "sampleId", source.getSampleId(),
                    "name", source.getName(),
                    "dataType", source.getDataType(),
                    "contentPreview", preview(payload.text()),
                    "metadata", readJson(source.getMetadataJson(), new TypeReference<Map<String, Object>>() {}, Map.of())
            ));
        }
        Map<String, Object> fused = new LinkedHashMap<>();
        fused.put("fusionStrategy", firstNonBlank(request.getFusionStrategy(), "timestamp"));
        fused.put("alignmentConfig", request.getAlignmentConfig() == null ? Map.of() : request.getAlignmentConfig());
        fused.put("createdAt", Instant.now().toString());
        fused.put("components", components);
        ProcessingPayload payload = textPayload(toJsonPretty(fused), "json", "application/json");
        String fusedId = saveDerivedSample(datasetId, payload, "fused-" + shortId() + ".json", "sensor",
                List.of("fused", firstNonBlank(request.getFusionStrategy(), "timestamp")), Map.of(
                        "derivedFrom", request.getSampleIds(),
                        "operation", "fusion",
                        "eventType", firstNonBlank(request.getFusionStrategy(), "timestamp"),
                        "timestamp", Instant.now().toEpochMilli()
                ));
        persistenceService.save(DOMAIN, TYPE_RESULT, fusedId, List.of(fusedId));
        publishCatalogChanged("DATA_FUSED", datasetId, 1);
        return List.of(fusedId);
    }

    @Transactional
    public List<String> generateScenario(ScenarioGenerationRequest request) {
        int count = Math.max(1, request.getTargetCount());
        String datasetId = request.getBaseDatasetId();
        if (datasetId == null || datasetId.isBlank()) {
            datasetId = createGeneratedDataset("scenario-" + shortId(), "text");
        } else {
            requireDataset(datasetId);
        }
        List<String> generatedIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> scenario = new LinkedHashMap<>();
            scenario.put("scenarioId", "scenario_" + shortId());
            scenario.put("description", firstNonBlank(request.getUserDescription(), "自动生成测试场景"));
            scenario.put("eventType", eventTypeFromDescription(request.getUserDescription(), i));
            scenario.put("timestamp", Instant.now().plusSeconds(i).toString());
            scenario.put("constraints", request.getConstraints() == null ? Map.of() : request.getConstraints());
            scenario.put("sample", Map.of(
                    "text", firstNonBlank(request.getUserDescription(), "自动生成测试场景") + " #" + (i + 1),
                    "imageHint", "synthetic-image-slot-" + (i + 1),
                    "audioHint", "synthetic-audio-slot-" + (i + 1),
                    "sensorHint", Map.of("speed", 20 + i, "temperature", 25 + i % 5)
            ));
            ProcessingPayload payload = textPayload(toJsonPretty(scenario), "json", "application/json");
            String sampleId = saveDerivedSample(datasetId, payload, "scenario-" + (i + 1) + "-" + shortId() + ".json", "text",
                    List.of("scenario", "generated"), Map.of(
                            "operation", "scenario-generation",
                            "eventType", scenario.get("eventType"),
                            "timestamp", Instant.now().toEpochMilli(),
                            "userDescription", firstNonBlank(request.getUserDescription(), "")
                    ));
            generatedIds.add(sampleId);
        }
        persistenceService.save(DOMAIN, TYPE_RESULT, "scenario-" + UUID.randomUUID().toString().replace("-", ""), generatedIds);
        publishCatalogChanged("SCENARIO_GENERATED", datasetId, generatedIds.size());
        return generatedIds;
    }

    private List<UploadFile> normalizeUploads(List<MultipartFile> files, List<String> relativePaths) {
        if (relativePaths != null && !relativePaths.isEmpty() && relativePaths.size() != files.size()) {
            throw new IllegalArgumentException("relativePaths数量必须与files一致");
        }
        List<UploadFile> uploads = new ArrayList<>();
        for (int index = 0; index < files.size(); index++) {
            MultipartFile file = files.get(index);
            String relativePath = relativePaths == null || relativePaths.isEmpty()
                    ? file.getOriginalFilename()
                    : relativePaths.get(index);
            String normalized = normalizeRelativePath(relativePath);
            String extension = extensionOf(normalized);
            if (!isSupportedExtension(extension)) {
                throw new IllegalArgumentException("数据集包含不支持的文件: " + normalized);
            }
            uploads.add(new UploadFile(file, normalized, pairingKey(normalized)));
        }
        return uploads;
    }

    private UploadPlan planUpload(List<UploadFile> uploads) {
        Map<String, UploadFile> images = new LinkedHashMap<>();
        Map<String, UploadFile> labels = new LinkedHashMap<>();
        List<UploadFile> standalone = new ArrayList<>();
        for (UploadFile upload : uploads) {
            String extension = extensionOf(upload.relativePath());
            if (IMAGE_EXTENSIONS.contains(extension)) {
                UploadFile duplicate = images.putIfAbsent(upload.pairKey(), upload);
                if (duplicate != null) {
                    throw new IllegalArgumentException("存在无法区分的同名图片: " + duplicate.relativePath() + " 和 " + upload.relativePath());
                }
            } else if (LABEL_EXTENSIONS.contains(extension)) {
                UploadFile duplicate = labels.putIfAbsent(upload.pairKey(), upload);
                if (duplicate != null) {
                    standalone.add(upload);
                }
            } else {
                standalone.add(upload);
            }
        }
        if (images.isEmpty()) {
            standalone.addAll(labels.values());
            return new UploadPlan(List.of(), standalone);
        }
        List<String> missingLabels = images.keySet().stream().filter(key -> !labels.containsKey(key)).toList();
        if (!missingLabels.isEmpty()) {
            throw new IllegalArgumentException("图片与标签未完全配对，缺少标签: " + missingLabels);
        }
        List<SamplePair> pairs = images.entrySet().stream()
                .map(entry -> new SamplePair(entry.getKey(), entry.getValue(), labels.get(entry.getKey())))
                .toList();
        labels.entrySet().stream()
                .filter(entry -> !images.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .forEach(standalone::add);
        return new UploadPlan(pairs, standalone);
    }

    private SampleRecord baseRecord(String datasetId, ManagedSampleStorageService.StoredSample stored,
                                    String name, String dataType, long now) {
        SampleRecord record = new SampleRecord();
        record.setSampleId(UUID.randomUUID().toString().replace("-", ""));
        record.setDatasetId(datasetId);
        record.setName(name);
        record.setDataType(dataType);
        record.setStorageKey(stored.storageKey());
        record.setOriginalName(stored.originalName());
        record.setContentType(firstNonBlank(stored.contentType(), contentTypeFor(stored.originalName())));
        record.setFileSize(stored.fileSize());
        record.setSha256(stored.sha256());
        record.setTagsJson("[]");
        record.setMetadataJson("{}");
        record.setVersion("v1");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return record;
    }

    private String saveDerivedSample(String datasetId, ProcessingPayload payload, String outputName, String dataType,
                                     List<String> tags, Map<String, Object> metadata) {
        ManagedSampleStorageService.StoredSample stored = sampleStorageService.storeBytes(
                payload.bytes(), outputName, firstNonBlank(payload.contentType(), contentTypeFor(outputName)));
        long now = Instant.now().toEpochMilli();
        SampleRecord record = baseRecord(datasetId, stored, outputName, dataType, now);
        record.setImageRelativePath("generated/" + outputName);
        record.setTagsJson(writeJson(tags));
        record.setMetadataJson(writeJson(metadata));
        record.setVersion(nextVersion(datasetId));
        sampleRecordRepository.save(record);
        incrementDataset(datasetId, dataType, 1);
        return record.getSampleId();
    }

    private ProcessingPayload applyProcessingStep(ProcessingPayload payload, SampleRecord source, ProcessingStep step) {
        String type = step.getType() == null ? "cleaning" : step.getType();
        Map<String, Object> config = step.getConfig() == null ? Map.of() : step.getConfig();
        if ("image".equals(source.getDataType()) && payload.image() != null) {
            return switch (type) {
                case "denoising" -> imagePayload(blur(payload.image()), payload.extension(), payload.contentType());
                case "standardization", "normalization" -> imagePayload(normalizeImage(payload.image()), payload.extension(), payload.contentType());
                case "format_conversion" -> convertImage(payload.image(), String.valueOf(config.getOrDefault("targetFormat", "png")));
                default -> imagePayload(payload.image(), payload.extension(), payload.contentType());
            };
        }
        if ("text".equals(source.getDataType()) || TEXT_EXTENSIONS.contains(payload.extension())) {
            String text = payload.text();
            String processed = switch (type) {
                case "denoising" -> text.replaceAll("[\\t ]+", " ").replaceAll("([!?.,;，。！？])\\1+", "$1");
                case "standardization" -> text.replace("\r\n", "\n").replace('\r', '\n').trim();
                case "normalization" -> normalizeNumericText(text);
                case "format_conversion" -> convertText(text, String.valueOf(config.getOrDefault("targetFormat", "json")));
                default -> cleanText(text);
            };
            String extension = "format_conversion".equals(type) ? String.valueOf(config.getOrDefault("targetFormat", "json")) : payload.extension();
            return textPayload(processed, extension, contentTypeFor("x." + extension));
        }
        String manifest = toJsonPretty(Map.of(
                "operation", type,
                "sourceSampleId", source.getSampleId(),
                "note", "二进制音视频样本已完成流程登记，原始字节保留",
                "sourceSize", payload.bytes().length
        ));
        return textPayload(manifest, "json", "application/json");
    }

    private ProcessingPayload applyAugmentation(ProcessingPayload payload, SampleRecord source, AugmentationConfig config, int round) {
        String method = config.getMethod() == null ? "noise_injection" : config.getMethod();
        if ("image".equals(source.getDataType()) && payload.image() != null) {
            BufferedImage image = switch (method) {
                case "crop" -> cropCenter(payload.image());
                case "flip" -> flipHorizontal(payload.image());
                case "noise_injection" -> addImageNoise(payload.image(), round + 1);
                case "color_transform" -> colorShift(payload.image(), 1.0f + (round + 1) * 0.08f);
                case "geometric_transform" -> rotateImage(payload.image());
                case "cutout" -> cutout(payload.image());
                case "mixup", "cutmix" -> colorShift(payload.image(), 0.88f);
                default -> payload.image();
            };
            return imagePayload(image, payload.extension(), payload.contentType());
        }
        if ("text".equals(source.getDataType()) || TEXT_EXTENSIONS.contains(payload.extension())) {
            String text = switch (method) {
                case "synonym_replacement" -> replaceSynonyms(payload.text());
                case "pronoun_replacement" -> payload.text().replace("他", "该对象").replace("她", "该对象").replace("it", "the object");
                case "sentence_transform" -> transformSentences(payload.text());
                case "back_translation" -> "[back-translation] " + transformSentences(payload.text());
                default -> injectTextNoise(payload.text(), round + 1);
            };
            return textPayload(text, payload.extension(), payload.contentType());
        }
        String text = toJsonPretty(Map.of(
                "sourceSampleId", source.getSampleId(),
                "augmentation", method,
                "round", round + 1,
                "note", "音频增强流程记录，原始音频保持可追溯"
        ));
        return textPayload(text, "json", "application/json");
    }

    private List<AugmentationConfig> defaultAugmentations(String dataType) {
        AugmentationConfig config = new AugmentationConfig();
        config.setMethod("image".equals(dataType) ? "flip" : "text".equals(dataType) ? "synonym_replacement" : "noise_injection");
        config.setParameters(Map.of());
        return List.of(config);
    }

    private List<String> processingSourceIds(DataProcessingRequest request) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        if (request.getSampleIds() != null) {
            request.getSampleIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(id -> !id.isBlank())
                    .forEach(ids::add);
        }
        if (request.getSampleId() != null && !request.getSampleId().isBlank()) {
            ids.add(request.getSampleId().trim());
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("sampleId或sampleIds不能为空");
        }
        return new ArrayList<>(ids);
    }

    private ProcessingStep defaultStep(String type) {
        ProcessingStep step = new ProcessingStep();
        step.setType(type);
        step.setConfig(Map.of());
        return step;
    }

    private ProcessingPayload readPayload(SampleRecord sample) {
        try {
            byte[] bytes = Files.readAllBytes(sampleStorageService.requireExisting(sample.getStorageKey()));
            String extension = extensionOf(sample.getOriginalName());
            if ("image".equals(sample.getDataType()) || IMAGE_EXTENSIONS.contains(extension)) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                if (image != null) {
                    return new ProcessingPayload(bytes, null, image, extension, firstNonBlank(sample.getContentType(), contentTypeFor(sample.getOriginalName())));
                }
            }
            return new ProcessingPayload(bytes, new String(bytes, StandardCharsets.UTF_8), null,
                    extension, firstNonBlank(sample.getContentType(), contentTypeFor(sample.getOriginalName())));
        } catch (Exception ex) {
            throw new IllegalStateException("读取样本失败: " + sample.getSampleId(), ex);
        }
    }

    private ProcessingPayload textPayload(String text, String extension, String contentType) {
        return new ProcessingPayload(text.getBytes(StandardCharsets.UTF_8), text, null, normalizeExtension(extension), contentType);
    }

    private ProcessingPayload imagePayload(BufferedImage image, String extension, String contentType) {
        String format = normalizeImageFormat(extension);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return new ProcessingPayload(output.toByteArray(), null, image, format, firstNonBlank(contentType, contentTypeFor("x." + format)));
        } catch (Exception ex) {
            throw new IllegalStateException("生成图像样本失败", ex);
        }
    }

    private ProcessingPayload convertImage(BufferedImage image, String targetFormat) {
        return imagePayload(image, targetFormat, contentTypeFor("x." + normalizeImageFormat(targetFormat)));
    }

    private String cleanText(String text) {
        return Arrays.stream(text.replace('\r', '\n').split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !Set.of("null", "none", "nan", "na").contains(line.toLowerCase(Locale.ROOT)))
                .collect(Collectors.joining("\n"));
    }

    private String normalizeNumericText(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : cleanText(text).split("\n")) {
            String[] parts = line.trim().split("\\s+");
            List<String> normalized = new ArrayList<>();
            for (int i = 0; i < parts.length; i++) {
                try {
                    double value = Double.parseDouble(parts[i]);
                    if (i > 0) {
                        value = Math.max(0.0, Math.min(1.0, value));
                    }
                    normalized.add(String.format(Locale.ROOT, "%.6f", value));
                } catch (NumberFormatException ex) {
                    normalized.add(parts[i]);
                }
            }
            lines.add(String.join(" ", normalized));
        }
        return String.join("\n", lines);
    }

    private String convertText(String text, String targetFormat) {
        String format = normalizeExtension(targetFormat);
        List<String> lines = Arrays.stream(cleanText(text).split("\n")).toList();
        if ("csv".equals(format)) {
            return lines.stream().map(line -> line.replace(' ', ',')).collect(Collectors.joining("\n"));
        }
        if ("json".equals(format)) {
            return toJsonPretty(Map.of("lineCount", lines.size(), "lines", lines));
        }
        return String.join("\n", lines);
    }

    private BufferedImage blur(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int r = 0, g = 0, b = 0, count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && ny >= 0 && nx < image.getWidth() && ny < image.getHeight()) {
                            Color c = new Color(image.getRGB(nx, ny));
                            r += c.getRed();
                            g += c.getGreen();
                            b += c.getBlue();
                            count++;
                        }
                    }
                }
                out.setRGB(x, y, new Color(r / count, g / count, b / count).getRGB());
            }
        }
        return out;
    }

    private BufferedImage normalizeImage(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return colorShift(out, 1.08f);
    }

    private BufferedImage cropCenter(BufferedImage image) {
        int marginX = Math.max(1, image.getWidth() / 10);
        int marginY = Math.max(1, image.getHeight() / 10);
        BufferedImage crop = image.getSubimage(marginX, marginY, image.getWidth() - marginX * 2, image.getHeight() - marginY * 2);
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(crop, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return out;
    }

    private BufferedImage flipHorizontal(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, image.getWidth(), 0, -image.getWidth(), image.getHeight(), null);
        g.dispose();
        return out;
    }

    private BufferedImage addImageNoise(BufferedImage image, int seed) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Random random = new Random(seed);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y));
                int noise = random.nextInt(21) - 10;
                out.setRGB(x, y, new Color(clamp(c.getRed() + noise), clamp(c.getGreen() + noise), clamp(c.getBlue() + noise)).getRGB());
            }
        }
        return out;
    }

    private BufferedImage colorShift(BufferedImage image, float factor) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color c = new Color(image.getRGB(x, y));
                out.setRGB(x, y, new Color(clamp(Math.round(c.getRed() * factor)),
                        clamp(Math.round(c.getGreen() * factor)), clamp(Math.round(c.getBlue() * factor))).getRGB());
            }
        }
        return out;
    }

    private BufferedImage rotateImage(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.rotate(Math.toRadians(3), image.getWidth() / 2.0, image.getHeight() / 2.0);
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return out;
    }

    private BufferedImage cutout(BufferedImage image) {
        BufferedImage out = normalizeImage(image);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.BLACK);
        int w = Math.max(1, image.getWidth() / 4);
        int h = Math.max(1, image.getHeight() / 4);
        g.fillRect((image.getWidth() - w) / 2, (image.getHeight() - h) / 2, w, h);
        g.dispose();
        return out;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private String replaceSynonyms(String text) {
        return text.replace("快速", "高速")
                .replace("检测", "识别")
                .replace("异常", "异常情况")
                .replace("normal", "regular")
                .replace("hard", "challenging");
    }

    private String transformSentences(String text) {
        return Arrays.stream(text.split("(?<=[。.!?！？])"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .sorted(Comparator.comparingInt(String::length))
                .collect(Collectors.joining(" "));
    }

    private String injectTextNoise(String text, int seed) {
        List<String> lines = new ArrayList<>(Arrays.asList(text.split("\n")));
        if (!lines.isEmpty()) {
            int index = Math.floorMod(seed, lines.size());
            lines.set(index, lines.get(index) + " #aug" + seed);
        }
        return String.join("\n", lines);
    }

    private boolean sampleMatchesKeyword(Sample sample, String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        if (sample.getName() != null && sample.getName().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        if (sample.getOriginalName() != null && sample.getOriginalName().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        if (sample.getTags() != null && sample.getTags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(lower))) {
            return true;
        }
        if (sample.getMetadata() != null && sample.getMetadata().toString().toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        if (TEXT_EXTENSIONS.contains(extensionOf(sample.getOriginalName()))) {
            try {
                String content = Files.readString(resolveSamplePath(sample.getSampleId()), StandardCharsets.UTF_8);
                return content.toLowerCase(Locale.ROOT).contains(lower);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private SampleRecord requireSample(String sampleId) {
        return sampleRecordRepository.findById(sampleId)
                .orElseThrow(() -> new IllegalArgumentException("样本不存在: " + sampleId));
    }

    private DataIngestResponse requireDataset(String datasetId) {
        DataIngestResponse dataset = datasetStore.get(datasetId);
        if (dataset == null) {
            dataset = datasetRecordRepository.findById(datasetId).map(this::toDataset).orElse(null);
        }
        if (dataset == null) {
            dataset = persistenceService.findOne(DOMAIN, TYPE_DATASET, datasetId, DataIngestResponse.class).orElse(null);
        }
        if (dataset == null) {
            throw new IllegalArgumentException("数据集不存在: " + datasetId);
        }
        return dataset;
    }

    private String createGeneratedDataset(String datasetName, String modality) {
        long now = Instant.now().toEpochMilli();
        DataIngestResponse response = DataIngestResponse.builder()
                .datasetId(UUID.randomUUID().toString().replace("-", ""))
                .datasetName(datasetName)
                .assetCount(0)
                .supportedModalities(List.of(modality))
                .version("v1")
                .status("READY")
                .createdAt(Instant.ofEpochMilli(now))
                .updatedAt(Instant.ofEpochMilli(now))
                .build();
        saveDatasetRecord(response);
        datasetStore.put(response.getDatasetId(), response);
        persistenceService.save(DOMAIN, TYPE_DATASET, response.getDatasetId(), response);
        return response.getDatasetId();
    }

    private void incrementDataset(String datasetId, String modality, int delta) {
        datasetRecordRepository.findById(datasetId).ifPresent(record -> {
            List<String> modalities = new ArrayList<>(readJson(record.getSupportedModalitiesJson(), new TypeReference<List<String>>() {}, List.of()));
            if (!modalities.contains(modality)) {
                modalities.add(modality);
            }
            record.setAssetCount(record.getAssetCount() + delta);
            record.setSupportedModalitiesJson(writeJson(modalities));
            record.setUpdatedAt(Instant.now().toEpochMilli());
            datasetRecordRepository.save(record);
            datasetStore.put(datasetId, toDataset(record));
        });
    }

    private String nextVersion(String datasetId) {
        long count = sampleRecordRepository.findByDatasetIdOrderByCreatedAtDesc(datasetId).size() + 1L;
        return "v" + count;
    }

    private List<String> mergeTags(SampleRecord source, List<String> extra) {
        LinkedHashSet<String> tags = new LinkedHashSet<>(readJson(source.getTagsJson(), new TypeReference<List<String>>() {}, List.of()));
        tags.addAll(extra);
        return new ArrayList<>(tags);
    }

    private Map<String, Object> derivedMetadata(SampleRecord source, String operation) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("operation", operation);
        metadata.put("sourceSampleId", source.getSampleId());
        metadata.put("sourceVersion", source.getVersion());
        metadata.put("eventType", readJson(source.getMetadataJson(), new TypeReference<Map<String, Object>>() {}, Map.of()).getOrDefault("eventType", operation));
        metadata.put("timestamp", Instant.now().toEpochMilli());
        return metadata;
    }

    private String inferModality(String uri) {
        String lower = uri.toLowerCase(Locale.ROOT);
        String extension = extensionOf(lower);
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "image";
        }
        if (AUDIO_EXTENSIONS.contains(extension)) {
            return "audio";
        }
        if (VIDEO_EXTENSIONS.contains(extension)) {
            return "video";
        }
        if (TEXT_EXTENSIONS.contains(extension)) {
            return lower.contains("sensor") ? "sensor" : "text";
        }
        return "sensor";
    }

    private void saveDatasetRecord(DataIngestResponse response) {
        DatasetRecord record = new DatasetRecord();
        record.setDatasetId(response.getDatasetId());
        record.setDatasetName(response.getDatasetName());
        record.setAssetCount(response.getAssetCount());
        record.setSupportedModalitiesJson(writeJson(response.getSupportedModalities()));
        record.setVersion(response.getVersion());
        record.setStatus(response.getStatus());
        record.setCreatedAt(response.getCreatedAt() == null ? Instant.now().toEpochMilli() : response.getCreatedAt().toEpochMilli());
        record.setUpdatedAt(response.getUpdatedAt() == null ? Instant.now().toEpochMilli() : response.getUpdatedAt().toEpochMilli());
        datasetRecordRepository.save(record);
    }

    private DataIngestResponse toDataset(DatasetRecord record) {
        return DataIngestResponse.builder()
                .datasetId(record.getDatasetId())
                .datasetName(record.getDatasetName())
                .assetCount(record.getAssetCount())
                .supportedModalities(readJson(record.getSupportedModalitiesJson(), new TypeReference<List<String>>() {}, List.of()))
                .version(record.getVersion())
                .status(record.getStatus())
                .createdAt(Instant.ofEpochMilli(record.getCreatedAt()))
                .updatedAt(Instant.ofEpochMilli(record.getUpdatedAt()))
                .build();
    }

    private Sample toSample(SampleRecord record) {
        return Sample.builder()
                .sampleId(record.getSampleId())
                .datasetId(record.getDatasetId())
                .name(record.getName())
                .dataType(record.getDataType())
                .contentUrl("/api/v1/data/samples/" + record.getSampleId() + "/content")
                .imageRelativePath(record.getImageRelativePath())
                .originalName(record.getOriginalName())
                .contentType(record.getContentType())
                .fileSize(record.getFileSize())
                .sha256(record.getSha256())
                .labelContentUrl(record.getLabelStorageKey() == null
                        ? null
                        : "/api/v1/data/samples/" + record.getSampleId() + "/label")
                .labelRelativePath(record.getLabelRelativePath())
                .labelOriginalName(record.getLabelOriginalName())
                .labelContentType(record.getLabelContentType())
                .labelFileSize(record.getLabelFileSize())
                .labelSha256(record.getLabelSha256())
                .tags(readJson(record.getTagsJson(), new TypeReference<List<String>>() {}, List.of()))
                .metadata(readJson(record.getMetadataJson(), new TypeReference<Map<String, Object>>() {}, Map.of()))
                .version(record.getVersion())
                .createdAt(Instant.ofEpochMilli(record.getCreatedAt()))
                .updatedAt(Instant.ofEpochMilli(record.getUpdatedAt()))
                .build();
    }

    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("上传文件缺少相对路径");
        }
        String normalized = relativePath.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains("../") || normalized.contains(":/")) {
            throw new IllegalArgumentException("非法文件相对路径: " + relativePath);
        }
        return normalized;
    }

    private String pairingKey(String relativePath) {
        String normalized = normalizeRelativePath(relativePath).toLowerCase(Locale.ROOT);
        int dot = normalized.lastIndexOf('.');
        String withoutExtension = dot < 0 ? normalized : normalized.substring(0, dot);
        return Arrays.stream(withoutExtension.split("/"))
                .filter(segment -> !segment.isBlank())
                .filter(segment -> !PAIRING_DIRECTORY_NAMES.contains(segment))
                .collect(Collectors.joining("/"));
    }

    private boolean isSupportedExtension(String extension) {
        return IMAGE_EXTENSIONS.contains(extension)
                || TEXT_EXTENSIONS.contains(extension)
                || AUDIO_EXTENSIONS.contains(extension)
                || VIDEO_EXTENSIONS.contains(extension);
    }

    private String extensionOf(String path) {
        int dot = path == null ? -1 : path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeExtension(String extension) {
        String normalized = extension == null ? "txt" : extension.toLowerCase(Locale.ROOT).replace(".", "");
        return normalized.isBlank() ? "txt" : normalized;
    }

    private String normalizeImageFormat(String extension) {
        String format = normalizeExtension(extension);
        return "jpg".equals(format) || "jpeg".equals(format) ? "jpg" : "png".equals(format) ? "png" : "png";
    }

    private String derivedName(String originalName, String suffix, String extension) {
        String name = originalName == null ? "sample" : originalName;
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        return base + "-" + suffix + "." + normalizeExtension(extension);
    }

    private String eventTypeFromPath(String path) {
        String normalized = path == null ? "general" : path.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);
        String[] parts = fileName.split("[_\\-.]");
        return parts.length == 0 || parts[0].isBlank() ? "general" : parts[0];
    }

    private String eventTypeFromDescription(String description, int index) {
        if (description == null || description.isBlank()) {
            return "scenario-" + (index + 1);
        }
        String cleaned = description.replaceAll("[^\\p{IsHan}A-Za-z0-9]+", "-");
        if (cleaned.length() > 24) {
            cleaned = cleaned.substring(0, 24);
        }
        return cleaned.isBlank() ? "scenario-" + (index + 1) : cleaned;
    }

    private String contentTypeFor(String fileName) {
        return switch (extensionOf(fileName)) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "webp" -> "image/webp";
            case "json" -> "application/json";
            case "csv" -> "text/csv";
            case "xml" -> "application/xml";
            case "wav" -> "audio/wav";
            case "mp3" -> "audio/mpeg";
            case "flac" -> "audio/flac";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "avi" -> "video/x-msvideo";
            default -> "text/plain";
        };
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private String preview(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 240 ? text : text.substring(0, 240);
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private String toJsonPretty(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("JSON生成失败", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("数据元信息序列化失败", ex);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type, T fallback) {
        try {
            return value == null || value.isBlank() ? fallback : objectMapper.readValue(value, type);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private void publishCatalogChanged(String operation, String datasetId, int sampleCount) {
        Map<String, Object> payload = Map.of(
                "operation", operation,
                "datasetId", datasetId,
                "sampleCount", sampleCount,
                "changedAt", Instant.now().toEpochMilli()
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    wsMessageRouter.broadcast(WsMessageType.DATA_CATALOG_CHANGED, payload);
                }
            });
        } else {
            wsMessageRouter.broadcast(WsMessageType.DATA_CATALOG_CHANGED, payload);
        }
    }

    private record UploadFile(MultipartFile file, String relativePath, String pairKey) {
    }

    private record SamplePair(String pairKey, UploadFile image, UploadFile label) {
    }

    private record UploadPlan(List<SamplePair> pairs, List<UploadFile> standalone) {
    }

    private record ProcessingPayload(byte[] bytes, String text, BufferedImage image, String extension, String contentType) {
    }
}
