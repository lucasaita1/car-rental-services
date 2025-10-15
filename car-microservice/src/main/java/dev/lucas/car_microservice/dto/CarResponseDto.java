package dev.lucas.car_microservice.dto;


import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarResponseDto {

    private Long id;
    private String model;
    private String color;
    private String plate;
    private int year;
    private LocalDate rentalDate;
    private LocalDate returnDate;
    private Long userId;
}
