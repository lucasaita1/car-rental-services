package dev.lucas.user_microservice.dtos;

import dev.lucas.user_microservice.util.InputSanitizer;

public record LoginRequest (String email, String password) {

    public LoginRequest {
        email = InputSanitizer.email(email);
    }
}
