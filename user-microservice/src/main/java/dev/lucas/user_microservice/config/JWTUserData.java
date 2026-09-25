package dev.lucas.user_microservice.config;

import dev.lucas.user_microservice.enums.UserRole;
import lombok.Builder;

import java.time.Instant;

@Builder
public record JWTUserData(Long id, String name, String email, String cpf, UserRole role,
                          String jti, long version, Instant expiresAt) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
