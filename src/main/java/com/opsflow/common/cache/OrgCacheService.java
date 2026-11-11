package com.opsflow.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class OrgCacheService {

    private static final Logger log = LoggerFactory.getLogger(OrgCacheService.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration ttl;

    public OrgCacheService(
        ObjectProvider<StringRedisTemplate> redis,
        ObjectMapper objectMapper,
        @Value("${opus.cache.enabled:true}") boolean enabled,
        @Value("${opus.cache.ttl-seconds:45}") long ttlSeconds
    ) {
        this.redis = redis.getIfAvailable();
        this.objectMapper = objectMapper;
        this.enabled = enabled && this.redis != null;
        this.ttl = Duration.ofSeconds(Math.max(ttlSeconds, 1));
    }

    public <T> T getOrLoad(UUID organizationId, String name, Class<T> type, Supplier<T> loader) {
        return getOrLoad(organizationId, name, objectMapper.constructType(type), loader);
    }

    public <T> T getOrLoad(UUID organizationId, String name, TypeReference<T> type, Supplier<T> loader) {
        return getOrLoad(organizationId, name, objectMapper.getTypeFactory().constructType(type), loader);
    }

    private <T> T getOrLoad(UUID organizationId, String name, com.fasterxml.jackson.databind.JavaType type, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }
        String key = key(organizationId, name);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, type);
            }
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Cache read failed for {}: {}", key, ex.getMessage());
        }

        T value = loader.get();
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Cache write failed for {}: {}", key, ex.getMessage());
        }
        return value;
    }

    public void evictOrganization(UUID organizationId) {
        if (!enabled || organizationId == null) {
            return;
        }
        try {
            redis.delete(key(organizationId, "dashboard"));
            redis.delete(key(organizationId, "low-stock"));
        } catch (RuntimeException ex) {
            log.warn("Cache evict failed for org {}: {}", organizationId, ex.getMessage());
        }
    }

    private static String key(UUID organizationId, String name) {
        return "org:" + organizationId + ":" + name;
    }
}
