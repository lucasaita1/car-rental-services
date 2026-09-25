package dev.lucas.user_microservice.dtos;

import dev.lucas.user_microservice.enums.UserRole;

public record UserResponse (Long id,
                            String name,
                            String email,
                            String cpf,
                            String cnh,
                            UserRole role) {
}
