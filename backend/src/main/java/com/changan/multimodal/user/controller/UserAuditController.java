package com.changan.multimodal.user.controller;

import com.changan.multimodal.common.api.ApiResponse;
import com.changan.multimodal.user.dto.LoginRequest;
import com.changan.multimodal.user.dto.LoginStatRecord;
import com.changan.multimodal.user.dto.LoginSummary;
import com.changan.multimodal.user.service.UserAuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserAuditController {

    private final UserAuditService userAuditService;

    @PostMapping("/mock-login")
    public ApiResponse<LoginStatRecord> mockLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userAuditService.recordLogin(request));
    }

    @GetMapping("/login-stats")
    public ApiResponse<List<LoginStatRecord>> list() {
        return ApiResponse.success(userAuditService.listRecords());
    }

    @GetMapping("/login-summary")
    public ApiResponse<LoginSummary> summary() {
        return ApiResponse.success(userAuditService.summary());
    }
}
