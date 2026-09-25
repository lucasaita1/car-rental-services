package dev.lucas.user_microservice.dtos;

import dev.lucas.user_microservice.util.InputSanitizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
        @NotBlank(message = "Informe o nome.")
        @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres.")
        String name,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "E-mail muito longo.")
        String email,

        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos.")
        String cpf,

        @Pattern(regexp = "\\d{11}", message = "CNH deve ter 11 dígitos.")
        String cnh) {

    public ProfileUpdateRequest {
        name = InputSanitizer.text(name);
        email = InputSanitizer.email(email);
        cpf = InputSanitizer.digits(cpf);
        cnh = InputSanitizer.digits(cnh);
    }
}
