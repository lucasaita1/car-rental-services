package dev.lucas.car_microservice.dto;

import dev.lucas.car_microservice.enums.RentalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RentalResponseDto {

    private Long id;
    private Long carId;
    private String carModel;
    private String carPlate;
    private Long userId;
    private String userName;
    private String userEmail;
    private LocalDate rentalDate;
    private LocalDate expectedReturnDate;
    private LocalDate returnDate;
    private RentalStatus status;
    private boolean overdue;
}
