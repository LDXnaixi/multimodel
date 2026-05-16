package com.changan.multimodal.data.service;

import com.changan.multimodal.data.dto.DataAsset;
import com.changan.multimodal.data.dto.DataIngestRequest;
import com.changan.multimodal.data.dto.DataIngestResponse;
import com.changan.multimodal.data.dto.DataPipelineOperation;
import com.changan.multimodal.data.dto.DataPipelineRequest;
import com.changan.multimodal.data.dto.DataPipelineResponse;
import com.changan.multimodal.realtime.dto.WsMessageType;
import com.changan.multimodal.realtime.service.WsMessageRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class DataPipelineService {

    private final Map<String, DataIngestResponse> datasetStore = new ConcurrentHashMap<>();
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
