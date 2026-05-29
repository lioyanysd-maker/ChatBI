package com.chatbi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "chatbi.security")
public class SecurityConfig {

    /** 为空时不启用鉴权 */
    private String apiKey = "";

    /** 查询结果缓存 TTL（分钟），0 表示关闭 */
    private int queryCacheTtlMinutes = 15;

    /** AES 加密密钥，生产环境务必配置 */
    private String encryptionKey = "";

    public boolean isAuthEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
