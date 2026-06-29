package com.changan.multimodal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.sample-storage.root=./target/test-sample-assets")
@AutoConfigureMockMvc
class DataCatalogInferenceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadsListsReadsAndInfersBySampleIdWithoutExposingServerPath() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files",
                "factory.jpg",
                "image/jpeg",
                "managed image".getBytes()
        );
        MockMultipartFile label = new MockMultipartFile(
                "files",
                "factory.txt",
                "text/plain",
                "0 0.5 0.5 0.2 0.2".getBytes()
        );

        String uploadBody = mockMvc.perform(multipart("/api/v1/data/datasets/upload")
                        .file(image)
                        .file(label)
                        .param("datasetName", "集成测试数据集")
                        .param("tags", "integration")
                        .param("relativePaths", "factory/images/factory.jpg", "factory/labels/factory.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.assetCount").value(1))
                .andReturn().getResponse().getContentAsString();

        JsonNode uploaded = objectMapper.readTree(uploadBody).path("data");
        String datasetId = uploaded.path("datasetId").asText();

        String sampleBody = mockMvc.perform(get("/api/v1/data/datasets/{datasetId}/samples", datasetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].datasetId").value(datasetId))
                .andExpect(jsonPath("$.data[0].contentUrl").isNotEmpty())
                .andExpect(jsonPath("$.data[0].labelContentUrl").isNotEmpty())
                .andExpect(jsonPath("$.data[0].labelOriginalName").value("factory.txt"))
                .andReturn().getResponse().getContentAsString();
        String sampleId = objectMapper.readTree(sampleBody).path("data").get(0).path("sampleId").asText();

        mockMvc.perform(get("/api/v1/data/samples/{sampleId}/content", sampleId))
                .andExpect(status().isOk())
                .andExpect(content().bytes("managed image".getBytes()));

        mockMvc.perform(get("/api/v1/data/samples/{sampleId}/label", sampleId))
                .andExpect(status().isOk())
                .andExpect(content().bytes("0 0.5 0.5 0.2 0.2".getBytes()));

        String requestJson = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("modelId", "yolov8-detection")
                .put("modality", "image")
                .set("inputs", objectMapper.createArrayNode().add(
                        objectMapper.createObjectNode().put("sampleId", sampleId)
                )));
        String inferenceBody = mockMvc.perform(post("/api/v1/inference/run")
                        .contentType("application/json")
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.outputs[0].extra.sampleId").value(sampleId))
                .andReturn().getResponse().getContentAsString();

        assertThat(inferenceBody).doesNotContain("sourceUri", "storageKey", "sample-assets");

        mockMvc.perform(get("/api/v1/data/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].datasetId").value(datasetId));
    }

    @Test
    void rejectsFolderWhenAnImageHasNoMatchingLabel() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "files",
                "orphan.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/data/datasets/upload")
                        .file(image)
                        .param("datasetName", "不完整数据集")
                        .param("relativePaths", "dataset/images/orphan.jpg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("缺少标签")));
    }
    @Test
    void labelOnlyFolderCanRunProcessingAugmentationFusionAndScenarioGeneration() throws Exception {
        MockMultipartFile labelA = new MockMultipartFile(
                "files",
                "A_normal_001.txt",
                "text/plain",
                "0 0.5 0.5 0.2 0.2\n\n1 1.2 -0.1 0.3 0.4".getBytes()
        );
        MockMultipartFile labelB = new MockMultipartFile(
                "files",
                "E_hard_002.txt",
                "text/plain",
                "0 0.3 0.3 0.1 0.1".getBytes()
        );

        String uploadBody = mockMvc.perform(multipart("/api/v1/data/datasets/upload")
                        .file(labelA)
                        .file(labelB)
                        .param("datasetName", "labels-train")
                        .param("tags", "label", "train")
                        .param("relativePaths", "labels/train/A_normal_001.txt", "labels/train/E_hard_002.txt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.assetCount").value(2))
                .andExpect(jsonPath("$.data.supportedModalities[0]").value("text"))
                .andReturn().getResponse().getContentAsString();
        String datasetId = objectMapper.readTree(uploadBody).path("data").path("datasetId").asText();

        String searchJson = objectMapper.writeValueAsString(objectMapper.createObjectNode()
                .put("datasetId", datasetId)
                .put("keyword", "1.2"));
        String sampleBody = mockMvc.perform(post("/api/v1/data/samples/search")
                        .contentType("application/json")
                        .content(searchJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String sampleId = objectMapper.readTree(sampleBody).path("data").get(0).path("sampleId").asText();

        String processJson = """
                {
                  "sampleId": "%s",
                  "steps": [
                    {"type": "cleaning", "config": {}},
                    {"type": "normalization", "config": {}},
                    {"type": "format_conversion", "config": {"targetFormat": "json"}}
                  ]
                }
                """.formatted(sampleId);
        String processBody = mockMvc.perform(post("/api/v1/data/processing")
                        .contentType("application/json")
                        .content(processJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.processedSampleIds.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String processedId = objectMapper.readTree(processBody).path("data").path("processedSampleIds").get(0).asText();

        String augmentJson = """
                {
                  "sampleIds": ["%s"],
                  "dataType": "text",
                  "configs": [{"method": "synonym_replacement", "parameters": {}}],
                  "augmentationFactor": 1
                }
                """.formatted(sampleId);
        mockMvc.perform(post("/api/v1/data/augmentation")
                        .contentType("application/json")
                        .content(augmentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        String fusionJson = """
                {
                  "sampleIds": ["%s", "%s"],
                  "fusionStrategy": "timestamp",
                  "alignmentConfig": {}
                }
                """.formatted(sampleId, processedId);
        mockMvc.perform(post("/api/v1/data/fusion")
                        .contentType("application/json")
                        .content(fusionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        String scenarioJson = """
                {
                  "userDescription": "低置信度标签测试场景",
                  "baseDatasetId": "%s",
                  "targetCount": 2,
                  "constraints": {"source": "integration-test"}
                }
                """.formatted(datasetId);
        mockMvc.perform(post("/api/v1/data/scenarios/generate")
                        .contentType("application/json")
                        .content(scenarioJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
