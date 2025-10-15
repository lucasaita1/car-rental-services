package dev.lucas.car_microservice.mapper;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.dto.CarResponseDto;
import dev.lucas.car_microservice.entity.CarModel;

public class CarMapper {

    public static CarModel toEntity(CarRequestDto dto) {
        CarModel car = new CarModel();
        car.setModel(dto.getModel());
        car.setColor(dto.getColor());
        car.setPlate(dto.getPlate());
        car.setYear(dto.getYear());
        car.setRentalDate(dto.getRentalDate());
        car.setReturnDate(dto.getReturnDate());
        car.setUserId(dto.getUserId());
        return car;
    }

    public static CarResponseDto toResponseDto(CarModel car) {
        CarResponseDto dto = new CarResponseDto();
        dto.setId(car.getId());
        dto.setModel(car.getModel());
        dto.setColor(car.getColor());
        dto.setPlate(car.getPlate());
        dto.setYear(car.getYear());
        dto.setRentalDate(car.getRentalDate());
        dto.setReturnDate(car.getReturnDate());
        dto.setUserId(car.getUserId());
        return dto;
    }

}
