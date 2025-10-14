package dev.lucas.car_microservice.controller;


import dev.lucas.car_microservice.dto.UserCacheDto;
import dev.lucas.car_microservice.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @PostMapping("/user")
    public String saveUserCache(@RequestBody UserCacheDto userCacheDto) {
        cacheService.saveUser(userCacheDto, Duration.ofMinutes(120));
        return "Cache de usuário salvo com sucesso no Redis!";
    }

    @GetMapping("/user/{id}")
    public UserCacheDto getUserCache(@PathVariable String id) {
        return cacheService.getUser(id); // retorna o DTO completo
    }
}