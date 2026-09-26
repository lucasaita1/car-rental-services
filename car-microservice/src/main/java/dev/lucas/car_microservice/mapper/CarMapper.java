package dev.lucas.car_microservice.mapper;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.dto.CarResponseDto;
import dev.lucas.car_microservice.entity.CarDetail;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.storage.FileStorageService;
import dev.lucas.car_microservice.util.InputSanitizer;

import java.util.List;

public class CarMapper {

    public static CarModel toEntity(CarRequestDto dto) {
        CarModel car = new CarModel();
        car.setModel(InputSanitizer.text(dto.getModel()));
        car.setColor(InputSanitizer.text(dto.getColor()));
        car.setPlate(InputSanitizer.plate(dto.getPlate()));
        car.setYear(dto.getYear());
        car.setDailyRate(dto.getDailyRate());
        car.setDetails(sanitizeDetails(dto.getDetails()));
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
        dto.setDailyRate(car.getDailyRate());
        dto.setDetails(car.getDetails() == null || car.getDetails().isEmpty() ? null : car.getDetails());
        dto.setRentalDate(car.getRentalDate());
        dto.setReturnDate(car.getReturnDate());
        dto.setUserId(car.getUserId());
        dto.setStatus(car.getStatus());
        dto.setPhotoUrl(FileStorageService.publicUrl(car.getPhotoPath()));
        return dto;
    }

    public static List<CarDetail> sanitizeDetails(List<CarDetail> details) {
        if (details == null) {
            return null;
        }
        List<CarDetail> clean = details.stream()
                .filter(detail -> detail != null)
                .map(detail -> new CarDetail(InputSanitizer.text(detail.label()), InputSanitizer.text(detail.value())))
                .filter(detail -> detail.label() != null && !detail.label().isEmpty()
                        && detail.value() != null && !detail.value().isEmpty())
                .toList();
        return clean.isEmpty() ? null : clean;
    }
}
