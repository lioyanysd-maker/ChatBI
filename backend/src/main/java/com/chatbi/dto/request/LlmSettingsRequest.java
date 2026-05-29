package com.chatbi.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LlmSettingsRequest {

    @NotBlank(message = "provider 不能为空")
    private String provider;

    @NotBlank(message = "baseUrl 不能为空")
    private String baseUrl;

    @NotBlank(message = "model 不能为空")
    private String model;

    /** 留空表示不修改已有 Key */
    private String apiKey;
}
