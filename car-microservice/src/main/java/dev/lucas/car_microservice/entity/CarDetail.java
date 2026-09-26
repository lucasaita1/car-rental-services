package dev.lucas.car_microservice.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CarDetail(
        @NotBlank(message = "Informe o nome do detalhe.")
        @Size(max = 40, message = "Nome do detalhe muito longo.")
        String label,

        @NotBlank(message = "Informe o valor do detalhe.")
        @Size(max = 80, message = "Valor do detalhe muito longo.")
        String value) {
}
