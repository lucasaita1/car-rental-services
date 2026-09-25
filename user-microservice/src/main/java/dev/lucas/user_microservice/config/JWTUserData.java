package dev.lucas.user_microservice.config;

import dev.lucas.user_microservice.enums.UserRole;
import lombok.Builder;

@Builder
public record JWTUserData(Long id, String name, String email, String cpf, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
