package com.chatbi.service.cache;

import com.chatbi.dto.response.QueryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis 为主、内存为备：Redis 不可用时读写仍可用本地缓存。
 */
@Slf4j
@RequiredArgsConstructor
public class FallbackQueryCacheStore implements QueryCacheStore {

    private final QueryCacheStore primary;
    private final QueryCacheStore fallback;

    @Override
    public Optional<QueryResponse> get(String key) {
        Optional<QueryResponse> hit = primary.get(key);
        if (hit.isPresent()) {
            return hit;
        }
        return fallback.get(key);
    }

    @Override
    public void put(String key, QueryResponse response, Duration ttl) {
        fallback.put(key, response, ttl);
        primary.put(key, response, ttl);
    }
}
