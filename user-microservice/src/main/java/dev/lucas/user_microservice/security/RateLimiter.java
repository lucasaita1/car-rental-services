package dev.lucas.user_microservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimiter {

    static final String PREFIX = "rl:";

    private final StringRedisTemplate redis;

    public long retryAfterSeconds(String key, int limit, Duration window) {
        String redisKey = PREFIX + key;
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count == null) {
                return 0;
            }
            if (count == 1) {
                redis.expire(redisKey, window);
            }
            if (count <= limit) {
                return 0;
            }
            Long ttl = redis.getExpire(redisKey);
            if (ttl == null || ttl < 0) {
                redis.expire(redisKey, window);
                return window.toSeconds();
            }
            return Math.max(ttl, 1);
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
