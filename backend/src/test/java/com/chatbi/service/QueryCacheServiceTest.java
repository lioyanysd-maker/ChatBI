package com.chatbi.service;

import com.chatbi.config.SecurityConfig;
import com.chatbi.dto.response.QueryResponse;
import com.chatbi.service.cache.InMemoryQueryCacheStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryCacheServiceTest {

    private QueryCacheService cacheService;

    @BeforeEach
    void setUp() {
        SecurityConfig config = new SecurityConfig();
        config.setQueryCacheTtlMinutes(15);
        cacheService = new QueryCacheService(config, new InMemoryQueryCacheStore());
    }

    @Test
    void storesAndReturnsCachedResponse() {
        QueryResponse response = QueryResponse.builder()
                .question("总共有多少个商品？")
                .answerType("query")
                .naturalExplanation("共有 5 个商品")
                .rowCount(1)
                .build();

        cacheService.put(1L, "总共有多少个商品？", "auto", "session_a", response);

        Optional<QueryResponse> cached = cacheService.get(1L, "总共有多少个商品？", "auto", "session_a");
        assertTrue(cached.isPresent());
        assertEquals("共有 5 个商品", cached.get().getNaturalExplanation());
    }

    @Test
    void cacheDisabledWhenTtlZero() {
        SecurityConfig config = new SecurityConfig();
        config.setQueryCacheTtlMinutes(0);
        QueryCacheService disabled = new QueryCacheService(config, new InMemoryQueryCacheStore());

        QueryResponse response = QueryResponse.builder().question("q").build();
        disabled.put(1L, "q", "auto", "s1", response);

        assertTrue(disabled.get(1L, "q", "auto", "s1").isEmpty());
    }
}
