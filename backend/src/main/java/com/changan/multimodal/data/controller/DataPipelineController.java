package com.changan.multimodal.data.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.data.dto.*;
import com.changan.multimodal.data.service.DataPipelineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class DataPipelineController {

    private final DataPipelineService dataPipelineService;

    @PostMapping("/datasets/register")
    public ApiResponse<DataIngestResponse> register(@Valid @RequestBody DataIngestRequest request) {
        return ApiResponse.success(dataPipelineService.registerDataset(request));
    }

    @PostMapping(value = "/datasets/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DataIngestResponse> uploadDataset(
            @RequestParam("datasetName") String datasetName,
            @RequestParam(value = "tags", required = false) List<String> tags,
            @RequestParam(value = "relativePaths", required = false) List<String> relativePaths,
            @RequestPart("files") List<MultipartFile> files) {
        return ApiResponse.success(dataPipelineService.uploadDataset(datasetName, tags, relativePaths, files));
    }

    @GetMapping("/datasets")
    public ApiResponse<List<DataIngestResponse>> listDatasets() {
        return ApiResponse.success(dataPipelineService.listDatasets());
    }

    @GetMapping("/datasets/{datasetId}/samples")
    public ApiResponse<List<Sample>> listDatasetSamples(@PathVariable String datasetId) {
        return ApiResponse.success(dataPipelineService.listDatasetSamples(datasetId));
    }

    @PostMapping("/pipelines/run")
    public ApiResponse<DataPipelineResponse> run(@Valid @RequestBody DataPipelineRequest request) {
        return ApiResponse.success(dataPipelineService.runPipeline(request));
    }

    @GetMapping("/datasources")
    public ApiResponse<List<DataSource>> listDataSources() {
        return ApiResponse.success(dataPipelineService.listDataSources());
    }

    @PostMapping("/datasources")
    public ApiResponse<DataSource> addDataSource(@Valid @RequestBody DataSource dataSource) {
        return ApiResponse.success(dataPipelineService.addDataSource(dataSource));
    }

    @PostMapping("/samples/search")
    public ApiResponse<List<Sample>> searchSamples(@Valid @RequestBody SampleSearchRequest request) {
        return ApiResponse.success(dataPipelineService.searchSamples(request));
    }

    @GetMapping("/samples/{sampleId}")
    public ApiResponse<Sample> getSample(@PathVariable String sampleId) {
        return ApiResponse.success(dataPipelineService.getSample(sampleId));
    }

    @GetMapping("/samples/{sampleId}/content")
    public ResponseEntity<Resource> sampleContent(@PathVariable String sampleId) {
        Resource resource = new FileSystemResource(dataPipelineService.resolveSamplePath(sampleId));
        String contentType = dataPipelineService.sampleContentType(sampleId);
        MediaType mediaType;
        try {
            mediaType = contentType == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(dataPipelineService.sampleOriginalName(sampleId), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @GetMapping("/samples/{sampleId}/label")
    public ResponseEntity<Resource> sampleLabel(@PathVariable String sampleId) {
        Resource resource = new FileSystemResource(dataPipelineService.requireSampleLabelPath(sampleId));
        String contentType = dataPipelineService.sampleLabelContentType(sampleId);
        MediaType mediaType;
        try {
            mediaType = contentType == null ? MediaType.TEXT_PLAIN : MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(dataPipelineService.sampleLabelOriginalName(sampleId), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @PostMapping("/processing")
    public ApiResponse<DataProcessingResponse> processData(@Valid @RequestBody DataProcessingRequest request) {
        return ApiResponse.success(dataPipelineService.processData(request));
    }

    @PostMapping("/augmentation")
    public ApiResponse<List<String>> augmentData(@Valid @RequestBody DataAugmentationRequest request) {
        return ApiResponse.success(dataPipelineService.augmentData(request));
    }

    @PostMapping("/fusion")
    public ApiResponse<List<String>> fuseData(@Valid @RequestBody DataFusionRequest request) {
        return ApiResponse.success(dataPipelineService.fuseData(request));
    }

    @PostMapping("/scenarios/generate")
    public ApiResponse<List<String>> generateScenario(@Valid @RequestBody ScenarioGenerationRequest request) {
        return ApiResponse.success(dataPipelineService.generateScenario(request));
    }
}
