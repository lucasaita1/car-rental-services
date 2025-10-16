package dev.lucas.car_microservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RentalEmailDto implements Serializable {
    private String userName;
    private String userEmail;
    private String userCpf;
    private String carModel;
    private String carPlate;
    private LocalDate rentalDate;
    private LocalDate returnDate;
}