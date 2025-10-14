package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.dto.UserCacheDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Salva usuário no Redis com TTL
    public void saveUser(UserCacheDto user, Duration ttl) {
        String key = "user:" + user.getId();
        redisTemplate.opsForValue().set(key, user, ttl);
    }

    // Recupera usuário completo pelo ID
    public UserCacheDto getUser(String id) {
        Object value = redisTemplate.opsForValue().get("user:" + id);
        if (value instanceof UserCacheDto) {
            return (UserCacheDto) value;
        }
        return null;
    }

}