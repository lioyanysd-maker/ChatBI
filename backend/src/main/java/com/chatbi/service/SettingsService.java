package com.chatbi.service;

import cn.hutool.core.util.StrUtil;
import com.chatbi.config.LLMConfig;
import com.chatbi.dto.request.LlmSettingsRequest;
import com.chatbi.dto.response.LlmSettingsResponse;
import com.chatbi.entity.SystemConfig;
import com.chatbi.exception.BusinessException;
import com.chatbi.util.SecretCryptoService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbi.mapper.SystemConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String LLM_CONFIG_KEY = "llm";

    private final LLMConfig llmConfig;
    private final SystemConfigMapper systemConfigMapper;
    private final ObjectMapper objectMapper;
    private final LLMService llmService;
    private final JdbcTemplate jdbcTemplate;
    private final SecretCryptoService secretCryptoService;

    @PostConstruct
    public void loadPersistedSettings() {
        ensureSystemConfigTable();
        try {
            SystemConfig stored = systemConfigMapper.selectById(LLM_CONFIG_KEY);
            if (stored == null || StrUtil.isBlank(stored.getConfigValue())) {
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = objectMapper.readValue(stored.getConfigValue(), Map.class);
            Object rawApiKey = map.get("apiKey");
            applyLlmMap(map, false);
            if (rawApiKey != null && StrUtil.isNotBlank(rawApiKey.toString())
                    && !secretCryptoService.isEncrypted(rawApiKey.toString())) {
                migratePlainApiKeyIfNeeded();
            }
        } catch (Exception ex) {
            log.warn("Failed to load persisted LLM settings: {}", ex.getMessage());
        }
    }

    public LlmSettingsResponse getLlmSettings() {
        return toResponse();
    }

    public LlmSettingsResponse updateLlmSettings(LlmSettingsRequest request) {
        if (StrUtil.isNotBlank(request.getApiKey())) {
            llmConfig.setApiKey(request.getApiKey().trim());
        } else if (!llmConfig.isConfigured()) {
            throw new BusinessException(400, "请填写 API Key");
        }
        llmConfig.setProvider(request.getProvider().trim());
        llmConfig.setBaseUrl(normalizeBaseUrl(request.getBaseUrl()));
        llmConfig.setModel(request.getModel().trim());
        persistLlmSettings();
        return toResponse();
    }

    public void testLlmSettings(LlmSettingsRequest request) {
        String apiKey = StrUtil.blankToDefault(request.getApiKey(), llmConfig.getApiKey());
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(400, "请先填写 API Key");
        }
        llmService.testConnection(
                request.getProvider(),
                normalizeBaseUrl(request.getBaseUrl()),
                request.getModel(),
                apiKey
        );
    }

    private void persistLlmSettings() {
        ensureSystemConfigTable();
        Map<String, String> payload = new HashMap<>();
        payload.put("provider", llmConfig.getProvider());
        payload.put("baseUrl", llmConfig.getBaseUrl());
        payload.put("model", llmConfig.getModel());
        payload.put("apiKey", secretCryptoService.encrypt(llmConfig.getApiKey()));

        SystemConfig config = new SystemConfig();
        config.setConfigKey(LLM_CONFIG_KEY);
        try {
            config.setConfigValue(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(500, "保存配置失败");
        }
        config.setUpdatedAt(LocalDateTime.now());

        try {
            if (systemConfigMapper.selectById(LLM_CONFIG_KEY) == null) {
                systemConfigMapper.insert(config);
            } else {
                systemConfigMapper.updateById(config);
            }
        } catch (Exception ex) {
            log.error("Failed to persist LLM settings", ex);
            throw new BusinessException(500, "保存配置失败，请确认 system_config 表已创建");
        }
    }

    private void ensureSystemConfigTable() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS system_config (
                        config_key VARCHAR(64) PRIMARY KEY COMMENT '配置键',
                        config_value TEXT COMMENT '配置值 JSON',
                        updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    )
                    """);
        } catch (Exception ex) {
            log.warn("Could not ensure system_config table: {}", ex.getMessage());
        }
    }

    private void applyLlmMap(Map<String, Object> map, boolean requireApiKey) {
        Object provider = map.get("provider");
        Object baseUrl = map.get("baseUrl");
        Object model = map.get("model");
        Object apiKey = map.get("apiKey");

        if (provider != null && StrUtil.isNotBlank(provider.toString())) {
            llmConfig.setProvider(provider.toString());
        }
        if (baseUrl != null && StrUtil.isNotBlank(baseUrl.toString())) {
            llmConfig.setBaseUrl(normalizeBaseUrl(baseUrl.toString()));
        }
        if (model != null && StrUtil.isNotBlank(model.toString())) {
            llmConfig.setModel(model.toString());
        }
        if (apiKey != null && StrUtil.isNotBlank(apiKey.toString())) {
            llmConfig.setApiKey(secretCryptoService.decrypt(apiKey.toString()));
        } else if (requireApiKey && !llmConfig.isConfigured()) {
            throw new BusinessException(400, "请填写 API Key");
        }
    }

    private void migratePlainApiKeyIfNeeded() {
        if (!llmConfig.isConfigured()) {
            return;
        }
        try {
            persistLlmSettings();
            log.info("Migrated LLM API Key to encrypted storage");
        } catch (Exception ex) {
            log.warn("Failed to migrate LLM API Key encryption: {}", ex.getMessage());
        }
    }

    private LlmSettingsResponse toResponse() {
        return LlmSettingsResponse.builder()
                .provider(llmConfig.getProvider())
                .baseUrl(llmConfig.getBaseUrl())
                .model(llmConfig.getModel())
                .apiKeyConfigured(llmConfig.isConfigured())
                .apiKeyMasked(maskApiKey(llmConfig.getApiKey()))
                .build();
    }

    private String maskApiKey(String apiKey) {
        if (StrUtil.isBlank(apiKey)) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
