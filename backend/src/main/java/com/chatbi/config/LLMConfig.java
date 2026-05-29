package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chatbi.llm")
public class LLMConfig {

    private String provider = "deepseek";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private String baseUrl = "https://api.deepseek.com/v1";
    private int timeoutMs = 60000;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
