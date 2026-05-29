package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chatbi.sql")
public class SqlConfig {

    private int maxLimit = 1000;
    private int queryTimeoutMs = 5000;
}
