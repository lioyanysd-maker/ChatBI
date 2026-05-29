package com.chatbi.service.cache;

import com.chatbi.dto.response.QueryResponse;

import java.time.Duration;
import java.util.Optional;

public interface QueryCacheStore {

    Optional<QueryResponse> get(String key);

    void put(String key, QueryResponse response, Duration ttl);
}
