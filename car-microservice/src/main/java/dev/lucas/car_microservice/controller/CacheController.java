package dev.lucas.car_microservice.controller;


import dev.lucas.car_microservice.dto.UserCacheDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/user")
    public String saveUserCache(@RequestBody UserCacheDto userCacheDto) {
        // Salva no Redis com uma chave única
        String key = "user:" + userCacheDto.getId();
        redisTemplate.opsForValue().set(key, userCacheDto);
        return "Cache de usuário salvo com sucesso no Redis!";
    }

    @GetMapping("/user/{id}")
    public Object getUserCache(@PathVariable String id) {
        return redisTemplate.opsForValue().get("user:" + id);
    }
}