package com.chatbi.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmSettingsResponse {

    private String provider;
    private String baseUrl;
    private String model;
    private boolean apiKeyConfigured;
    private String apiKeyMasked;
}
