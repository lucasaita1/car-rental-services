package dev.lucas.car_microservice.mapper;

import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.entity.RentalModel;

import java.time.LocalDate;

public class RentalMapper {

    private RentalMapper() {
    }

    public static RentalResponseDto toResponseDto(RentalModel rental, LocalDate reference) {
        return RentalResponseDto.builder()
                .id(rental.getId())
                .carId(rental.getCarId())
                .carModel(rental.getCarModel())
                .carPlate(rental.getCarPlate())
                .userId(rental.getUserId())
                .userName(rental.getUserName())
                .userEmail(rental.getUserEmail())
                .rentalDate(rental.getRentalDate())
                .expectedReturnDate(rental.getExpectedReturnDate())
                .returnDate(rental.getReturnDate())
                .status(rental.getStatus())
                .overdue(rental.isOverdue(reference))
                .build();
        // O CPF fica de fora de propósito: é dado pessoal e não tem uso nas
        // telas de consulta, apenas no e-mail de confirmação da locação.
    }
}
