package com.changan.multimodal.data.service;

import com.changan.multimodal.data.dto.*;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataPipelineService {

    private final Map<String, DataIngestResponse> datasetStore = new ConcurrentHashMap<>();
    private final Map<String, Sample> sampleStore = new ConcurrentHashMap<>();
    private final Map<String, DataSource> dataSourceStore = new ConcurrentHashMap<>();
    private final WsMessageRouter wsMessageRouter;

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
                .build();
        datasetStore.put(datasetId, response);
        return response;
    }

    public DataPipelineResponse runPipeline(DataPipelineRequest request) {
        DataIngestResponse dataset = datasetStore.get(request.getDatasetId());
        if (dataset == null) {
            throw new IllegalArgumentException("数据集不存在: " + request.getDatasetId());
        }
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
        return response;
    }

    public List<DataSource> listDataSources() {
        return new ArrayList<>(dataSourceStore.values());
    }

    public DataSource addDataSource(DataSource dataSource) {
        dataSource.setSourceId(UUID.randomUUID().toString().replace("-", ""));
        dataSource.setConnectedAt(Instant.now());
        dataSource.setStatus("CONNECTED");
        dataSourceStore.put(dataSource.getSourceId(), dataSource);
        return dataSource;
    }

    public List<Sample> searchSamples(SampleSearchRequest request) {
        return sampleStore.values().stream()
                .filter(sample -> request.getKeyword() == null || 
                                 sample.getName().contains(request.getKeyword()))
                .filter(sample -> request.getDataType() == null || 
                                 sample.getDataType().equals(request.getDataType()))
                .filter(sample -> request.getTags() == null || request.getTags().isEmpty() ||
                                 sample.getTags().stream().anyMatch(tag -> request.getTags().contains(tag)))
                .collect(Collectors.toList());
    }

    public Sample addSample(Sample sample) {
        sample.setSampleId(UUID.randomUUID().toString().replace("-", ""));
        sample.setCreatedAt(Instant.now());
        sample.setUpdatedAt(Instant.now());
        sample.setVersion("v1");
        sampleStore.put(sample.getSampleId(), sample);
        return sample;
    }

    public DataProcessingResponse processData(DataProcessingRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        List<String> processedIds = new ArrayList<>();
        processedIds.add(request.getSampleId() + "_processed");
        
        Map<String, Object> results = new HashMap<>();
        results.put("taskId", taskId);
        results.put("originalSampleId", request.getSampleId());
        
        return DataProcessingResponse.builder()
                .taskId(taskId)
                .status("COMPLETED")
                .processedSampleIds(processedIds)
                .results(results)
                .build();
    }

    public List<String> augmentData(DataAugmentationRequest request) {
        List<String> augmentedIds = new ArrayList<>();
        for (int i = 0; i < request.getAugmentationFactor(); i++) {
            for (String sampleId : request.getSampleIds()) {
                String newId = sampleId + "_aug_" + i;
                augmentedIds.add(newId);
            }
        }
        return augmentedIds;
    }

    public List<String> fuseData(DataFusionRequest request) {
        List<String> fusedIds = new ArrayList<>();
        String fusedId = "fused_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        fusedIds.add(fusedId);
        return fusedIds;
    }

    public List<String> generateScenario(ScenarioGenerationRequest request) {
        List<String> generatedIds = new ArrayList<>();
        for (int i = 0; i < request.getTargetCount(); i++) {
            generatedIds.add("scenario_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        }
        return generatedIds;
    }

    private String inferModality(String uri) {
        String lower = uri.toLowerCase();
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".bmp")) {
            return "image";
        }
        if (lower.endsWith(".wav") || lower.endsWith(".mp3") || lower.endsWith(".flac")) {
            return "audio";
        }
        if (lower.endsWith(".json") || lower.endsWith(".txt") || lower.endsWith(".csv")) {
            return "text";
        }
        return "sensor";
    }
}
