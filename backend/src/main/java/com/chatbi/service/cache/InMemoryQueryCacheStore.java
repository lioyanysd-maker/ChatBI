package com.chatbi.service.cache;

import com.chatbi.dto.response.QueryResponse;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryQueryCacheStore implements QueryCacheStore {

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<QueryResponse> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.isExpired()) {
            cache.remove(key, entry);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    @Override
    public void put(String key, QueryResponse response, Duration ttl) {
        evictExpiredIfNeeded();
        cache.put(key, new CacheEntry(response, System.currentTimeMillis() + ttl.toMillis()));
    }

    private void evictExpiredIfNeeded() {
        if (cache.size() < 200) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getValue().expireAtMs() <= now) {
                iterator.remove();
            }
        }
    }

    private record CacheEntry(QueryResponse response, long expireAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() > expireAtMs;
        }
    }
}
