package dev.lucas.car_microservice.dto;

import dev.lucas.car_microservice.entity.CarDetail;
import dev.lucas.car_microservice.enums.CarStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarRequestDto {

    @NotBlank(message = "Informe o modelo.")
    @Size(max = 80, message = "Modelo muito longo.")
    private String model;

    @NotBlank(message = "Informe a cor.")
    @Size(max = 40, message = "Cor muito longa.")
    private String color;

    @NotBlank(message = "Informe a placa.")
    @Pattern(regexp = "^\\s*[A-Za-z]{3}-?\\d[A-Za-z0-9]\\d{2}\\s*$", message = "Placa inválida. Use ABC-1234 ou ABC1D23.")
    private String plate;

    @Min(value = 1950, message = "Ano deve ser a partir de 1950.")
    @Max(value = 2100, message = "Ano inválido.")
    private int year;

    @NotNull(message = "Informe o valor da diária.")
    @DecimalMin(value = "1.00", message = "A diária deve ser de pelo menos R$ 1,00.")
    @Digits(integer = 8, fraction = 2, message = "Diária inválida: use até 2 casas decimais.")
    private BigDecimal dailyRate;

    @Size(max = 20, message = "Informe no máximo 20 detalhes.")
    private List<@Valid CarDetail> details;

    private LocalDate rentalDate;
    private LocalDate returnDate;
    private Long userId;
    private CarStatus status;
}
