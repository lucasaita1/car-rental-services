package dev.lucas.user_microservice.dtos;

import dev.lucas.user_microservice.util.InputSanitizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "E-mail inválido.")
        String email) {

    public ForgotPasswordRequest {
        email = InputSanitizer.email(email);
    }
}
