package com.chatbi.service;

import cn.hutool.core.util.StrUtil;
import com.chatbi.config.LLMConfig;
import com.chatbi.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMService {

    private final LLMConfig llmConfig;
    private final RestTemplate llmRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String chat(String prompt) {
        if (!llmConfig.isConfigured()) {
            throw new BusinessException(401, "大模型 API Key 未配置，请在右上角设置中配置");
        }
        return chatWithConfig(
                llmConfig.getProvider(),
                llmConfig.getBaseUrl(),
                llmConfig.getModel(),
                llmConfig.getApiKey(),
                prompt
        );
    }

    public void chatStream(String prompt, Consumer<String> onDelta) {
        if (!llmConfig.isConfigured()) {
            throw new BusinessException(401, "大模型 API Key 未配置，请在右上角设置中配置");
        }
        chatStreamWithConfig(
                llmConfig.getBaseUrl(),
                llmConfig.getModel(),
                llmConfig.getApiKey(),
                prompt,
                onDelta
        );
    }

    public void testConnection(String provider, String baseUrl, String model, String apiKey) {
        chatWithConfig(provider, baseUrl, model, apiKey, "请回复：连接成功");
    }

    private String chatWithConfig(String provider, String baseUrl, String model, String apiKey, String prompt) {
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(401, "大模型 API Key 未配置，请在右上角设置中配置");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );

            ResponseEntity<String> response = llmRestTemplate.postForEntity(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(500, "大模型返回内容为空");
            }
            return content.asText();
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("LLM call failed", ex);
            throw new BusinessException(500, "大模型调用失败：" + ex.getMessage());
        } catch (Exception ex) {
            log.error("LLM parse failed", ex);
            throw new BusinessException(500, "大模型响应解析失败");
        }
    }

    private void chatStreamWithConfig(String baseUrl, String model, String apiKey, String prompt,
                                      Consumer<String> onDelta) {
        if (StrUtil.isBlank(apiKey)) {
            throw new BusinessException(401, "大模型 API Key 未配置，请在右上角设置中配置");
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(baseUrl + "/chat/completions").toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(Math.max(llmConfig.getTimeoutMs(), 5000));
            connection.setReadTimeout(Math.max(llmConfig.getTimeoutMs(), 5000));
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Accept", "text/event-stream");

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("stream", true);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            try (OutputStream os = connection.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                throw new BusinessException(500, "大模型流式调用失败：HTTP " + status);
            }

            if (status >= 400) {
                String errorBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                throw new BusinessException(500, "大模型流式调用失败：" + errorBody);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring(5).trim();
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    JsonNode root = objectMapper.readTree(payload);
                    JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                    if (!delta.isMissingNode()) {
                        String text = delta.asText();
                        if (!text.isEmpty()) {
                            onDelta.accept(text);
                        }
                    }
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.error("LLM stream failed", ex);
            throw new BusinessException(500, "大模型流式调用失败：" + ex.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
