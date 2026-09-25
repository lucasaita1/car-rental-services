package dev.lucas.car_microservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenRevocationChecker {

    static final String REVOKED_PREFIX = "auth:revoked:";
    static final String VERSION_PREFIX = "auth:token-version:";

    private final StringRedisTemplate redis;

    public boolean isRevoked(AuthenticatedUser user) {
        try {
            if (user.jti() == null || Boolean.TRUE.equals(redis.hasKey(REVOKED_PREFIX + user.jti()))) {
                return true;
            }
            String current = redis.opsForValue().get(VERSION_PREFIX + user.id());
            return current != null && user.version() < Long.parseLong(current);
        } catch (RuntimeException e) {
            return true;
        }
    }
}
