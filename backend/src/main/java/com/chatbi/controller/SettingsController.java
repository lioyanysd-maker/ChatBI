package com.chatbi.controller;

import com.chatbi.dto.request.LlmSettingsRequest;
import com.chatbi.dto.response.ApiResponse;
import com.chatbi.dto.response.LlmSettingsResponse;
import com.chatbi.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chatbi/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/llm")
    public ApiResponse<LlmSettingsResponse> getLlmSettings() {
        return ApiResponse.ok(settingsService.getLlmSettings());
    }

    @PutMapping("/llm")
    public ApiResponse<LlmSettingsResponse> updateLlmSettings(@Valid @RequestBody LlmSettingsRequest request) {
        return ApiResponse.ok(settingsService.updateLlmSettings(request));
    }

    @PostMapping("/llm/test")
    public ApiResponse<Map<String, Object>> testLlmSettings(@Valid @RequestBody LlmSettingsRequest request) {
        settingsService.testLlmSettings(request);
        return ApiResponse.ok(Map.of("success", true, "message", "连接成功"));
    }
}
