package com.chatbi.config;

import com.chatbi.service.cache.FallbackQueryCacheStore;
import com.chatbi.service.cache.InMemoryQueryCacheStore;
import com.chatbi.service.cache.QueryCacheStore;
import com.chatbi.service.cache.RedisQueryCacheStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(prefix = "chatbi.cache", name = "redis-enabled", havingValue = "false")
    public QueryCacheStore memoryOnlyQueryCacheStore() {
        return new InMemoryQueryCacheStore();
    }

    @Bean
    @ConditionalOnProperty(prefix = "chatbi.cache", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public QueryCacheStore redisFallbackQueryCacheStore(StringRedisTemplate redisTemplate,
                                                        ObjectMapper objectMapper) {
        InMemoryQueryCacheStore memory = new InMemoryQueryCacheStore();
        RedisQueryCacheStore redisStore = new RedisQueryCacheStore(redisTemplate, objectMapper);
        return new FallbackQueryCacheStore(redisStore, memory);
    }
}
