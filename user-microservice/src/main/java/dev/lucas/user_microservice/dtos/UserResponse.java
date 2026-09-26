package dev.lucas.user_microservice.dtos;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.storage.FileStorageService;

import java.time.Instant;

public record UserResponse (Long id,
                            String name,
                            String email,
                            String cpf,
                            String cnh,
                            UserRole role,
                            String photoUrl,
                            boolean cnhDocument,
                            Instant cnhDocumentUploadedAt) {

    public static UserResponse from(UserModel user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getCnh(),
                user.getRole(),
                FileStorageService.publicUrl(user.getPhotoPath()),
                user.hasCnhDocument(),
                user.getCnhDocumentUploadedAt());
    }
}
