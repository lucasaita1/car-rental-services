package dev.lucas.car_microservice.controller;


import dev.lucas.car_microservice.dto.UserCacheDto;
import dev.lucas.car_microservice.security.AuthenticatedUser;
import dev.lucas.car_microservice.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheService cacheService;

    @PostMapping("/user")
    public String saveUserCache(@RequestBody UserCacheDto userCacheDto,
                                @AuthenticationPrincipal AuthenticatedUser principal) {
        requireCanActFor(principal, userCacheDto.getId());
        cacheService.saveUser(userCacheDto, Duration.ofMinutes(120));
        return "Cache de usuário salvo com sucesso no Redis!";
    }

    @GetMapping("/user/{id}")
    public UserCacheDto getUserCache(@PathVariable String id,
                                     @AuthenticationPrincipal AuthenticatedUser principal) {
        requireCanActFor(principal, id);
        return cacheService.getUser(id); // retorna o DTO completo
    }

    private void requireCanActFor(AuthenticatedUser principal, String userId) {
        if (!principal.isAdmin() && !String.valueOf(principal.id()).equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
