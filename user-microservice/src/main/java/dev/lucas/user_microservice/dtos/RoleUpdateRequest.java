package dev.lucas.user_microservice.dtos;

import dev.lucas.user_microservice.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull UserRole role) {
}
