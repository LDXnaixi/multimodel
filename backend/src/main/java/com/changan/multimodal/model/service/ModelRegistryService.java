package com.changan.multimodal.model.service;

import com.changan.multimodal.model.dto.ModelDescriptor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ModelRegistryService {

    public List<ModelDescriptor> listModels() {
        return List.of(
                ModelDescriptor.builder()
                        .modelId("yolov8-demo")
                        .modelName("YOLOv8")
                        .version("todo")
                        .algorithmType("object-detection")
                        .deploymentStatus("TODO_INTEGRATION")
                        .supportedModalities(List.of("image", "video"))
                        .todo("待接入真实模型服务与评估逻辑")
                        .build(),
                ModelDescriptor.builder()
                        .modelId("whisper-demo")
                        .modelName("Whisper")
                        .version("todo")
                        .algorithmType("speech-to-text")
                        .deploymentStatus("TODO_INTEGRATION")
                        .supportedModalities(List.of("audio"))
                        .todo("待接入真实音频转录服务")
                        .build()
        );
    }
}
