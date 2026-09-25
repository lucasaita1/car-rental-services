package dev.lucas.user_microservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenRevocationService {

    static final String REVOKED_PREFIX = "auth:revoked:";
    static final String VERSION_PREFIX = "auth:token-version:";

    private final StringRedisTemplate redis;

    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (jti != null && !ttl.isNegative() && !ttl.isZero()) {
            redis.opsForValue().set(REVOKED_PREFIX + jti, "1", ttl);
        }
    }

    public void revokeAll(Long userId) {
        redis.opsForValue().increment(VERSION_PREFIX + userId);
    }

    public long currentVersion(Long userId) {
        try {
            String value = redis.opsForValue().get(VERSION_PREFIX + userId);
            return value == null ? 0 : Long.parseLong(value);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Serviço de autenticação indisponível.");
        }
    }

    public boolean isRevoked(String jti, Long userId, long version) {
        try {
            if (jti == null || Boolean.TRUE.equals(redis.hasKey(REVOKED_PREFIX + jti))) {
                return true;
            }
            String current = redis.opsForValue().get(VERSION_PREFIX + userId);
            return current != null && version < Long.parseLong(current);
        } catch (RuntimeException e) {
            return true;
        }
    }
}
