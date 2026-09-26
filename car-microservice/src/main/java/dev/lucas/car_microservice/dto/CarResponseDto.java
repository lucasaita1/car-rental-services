package dev.lucas.car_microservice.dto;


import dev.lucas.car_microservice.entity.CarDetail;
import dev.lucas.car_microservice.enums.CarStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    private BigDecimal dailyRate;
    private List<CarDetail> details;
    private LocalDate rentalDate;
    private LocalDate returnDate;
    private Long userId;
    private CarStatus status;
    private String photoUrl;
    private boolean reserved;
}
