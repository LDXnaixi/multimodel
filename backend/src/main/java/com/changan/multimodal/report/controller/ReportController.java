package com.changan.multimodal.report.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.report.dto.ReportSummary;
import com.changan.multimodal.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{taskId}")
    public ApiResponse<ReportSummary> get(@PathVariable String taskId) {
        return ApiResponse.success(reportService.getReport(taskId));
    }

    @GetMapping("/{taskId}/export")
    public ResponseEntity<byte[]> export(@PathVariable String taskId, @RequestParam(defaultValue = "JSON") String format) {
        ReportService.ReportExport export = reportService.export(taskId, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + export.fileName())
                .contentType(MediaType.parseMediaType(export.contentType()))
                .body(export.content());
    }
}
