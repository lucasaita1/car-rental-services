package dev.lucas.user_microservice.dtos;

public record UserResponse (Long id,
                            String name,
                            String email,
                            String cpf,
                            String cnh) {
}
