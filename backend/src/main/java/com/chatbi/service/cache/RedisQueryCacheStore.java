package com.chatbi.service.cache;

import com.chatbi.dto.response.QueryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class RedisQueryCacheStore implements QueryCacheStore {

    static final String KEY_PREFIX = "chatbi:query:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<QueryResponse> get(String key) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, QueryResponse.class));
        } catch (Exception ex) {
            log.warn("Redis query cache get failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, QueryResponse response, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(KEY_PREFIX + key, json, ttl);
        } catch (JsonProcessingException ex) {
            log.warn("Redis query cache serialize failed: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis query cache put failed: {}", ex.getMessage());
        }
    }
}
