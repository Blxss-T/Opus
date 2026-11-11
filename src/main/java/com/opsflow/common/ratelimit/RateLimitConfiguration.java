package com.opsflow.common.ratelimit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RateLimitConfiguration {

    @Bean
    public RateLimiter rateLimiter(
        ObjectProvider<StringRedisTemplate> redis,
        @Value("${security.rate-limit.fail-closed:false}") boolean failClosed
    ) {
        return new RedisBackedRateLimiter(
            redis.getIfAvailable(),
            new SlidingWindowRateLimiter(),
            failClosed
        );
    }
}
