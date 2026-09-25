package dev.lucas.car_microservice.mapper;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.dto.CarResponseDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.storage.FileStorageService;
import dev.lucas.car_microservice.util.InputSanitizer;

public class CarMapper {

    public static CarModel toEntity(CarRequestDto dto) {
        CarModel car = new CarModel();
        car.setModel(InputSanitizer.text(dto.getModel()));
        car.setColor(InputSanitizer.text(dto.getColor()));
        car.setPlate(InputSanitizer.plate(dto.getPlate()));
        car.setYear(dto.getYear());
        car.setRentalDate(dto.getRentalDate());
        car.setReturnDate(dto.getReturnDate());
        car.setUserId(dto.getUserId());
        car.setStatus(dto.getStatus() != null ? dto.getStatus() : CarStatus.AVAILABLE);
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
        dto.setStatus(car.getStatus());
        dto.setPhotoUrl(FileStorageService.publicUrl(car.getPhotoPath()));
        return dto;
    }

}
