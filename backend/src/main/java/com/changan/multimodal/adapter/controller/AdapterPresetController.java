package com.changan.multimodal.adapter.controller;

import com.changan.multimodal.adapter.dto.AdapterPreset;
import com.changan.multimodal.adapter.dto.AdapterPresetRequest;
import com.changan.multimodal.adapter.service.AdapterPresetService;
import com.changan.multimodal.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/adapters/presets")
@RequiredArgsConstructor
public class AdapterPresetController {
    private final AdapterPresetService adapterPresetService;

    @GetMapping
    public ApiResponse<List<AdapterPreset>> list() {
        return ApiResponse.success(adapterPresetService.listPresets());
    }

    @GetMapping("/{presetId}")
    public ApiResponse<AdapterPreset> detail(@PathVariable String presetId) {
        return ApiResponse.success(adapterPresetService.findPreset(presetId));
    }

    @PostMapping
    public ApiResponse<AdapterPreset> create(@RequestBody AdapterPresetRequest request) {
        request.setCustom(true);
        return ApiResponse.success(adapterPresetService.savePreset(request));
    }

    @PutMapping("/{presetId}")
    public ApiResponse<AdapterPreset> update(@PathVariable String presetId, @RequestBody AdapterPresetRequest request) {
        request.setPresetId(presetId);
        return ApiResponse.success(adapterPresetService.savePreset(request));
    }

    @DeleteMapping("/{presetId}")
    public ApiResponse<Void> delete(@PathVariable String presetId) {
        adapterPresetService.deletePreset(presetId);
        return ApiResponse.success(null);
    }
}
