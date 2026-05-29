package com.chatbi.service;

import com.chatbi.config.SecurityConfig;
import com.chatbi.dto.response.QueryResponse;
import com.chatbi.service.cache.QueryCacheStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryCacheService {

    private final SecurityConfig securityConfig;
    private final QueryCacheStore queryCacheStore;

    public Optional<QueryResponse> get(Long dataSourceId, String question, String chartType, String sessionId) {
        if (securityConfig.getQueryCacheTtlMinutes() <= 0) {
            return Optional.empty();
        }
        String key = buildKey(dataSourceId, question, chartType, sessionId);
        Optional<QueryResponse> cached = queryCacheStore.get(key);
        if (cached.isPresent()) {
            log.debug("Query cache hit: {}", key);
        }
        return cached;
    }

    public void put(Long dataSourceId, String question, String chartType, String sessionId, QueryResponse response) {
        if (securityConfig.getQueryCacheTtlMinutes() <= 0 || response == null) {
            return;
        }
        String key = buildKey(dataSourceId, question, chartType, sessionId);
        Duration ttl = Duration.ofMinutes(securityConfig.getQueryCacheTtlMinutes());
        queryCacheStore.put(key, response, ttl);
    }

    private String buildKey(Long dataSourceId, String question, String chartType, String sessionId) {
        String raw = String.valueOf(dataSourceId) + "|"
                + (sessionId == null ? "" : sessionId) + "|"
                + (chartType == null ? "auto" : chartType) + "|"
                + question.trim().toLowerCase();
        return sha256(raw);
    }

    private String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            return raw;
        }
    }
}
