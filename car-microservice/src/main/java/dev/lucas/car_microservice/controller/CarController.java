package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.dto.CarResponseDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.mapper.CarMapper;
import dev.lucas.car_microservice.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @PostMapping
    public ResponseEntity<CarResponseDto> createCar(@Valid @RequestBody CarRequestDto carRequestDto) {
        CarModel carModel = CarMapper.toEntity(carRequestDto);
        CarModel savedCar = carService.save(carModel);
        CarResponseDto responseDto = CarMapper.toResponseDto(savedCar);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarResponseDto> getCarById(@PathVariable Long id) {
        CarModel carModel = carService.findById(id);
        CarResponseDto responseDto = CarMapper.toResponseDto(carModel);
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<CarResponseDto>> getAllCars() {
        List<CarModel> cars = carService.findAll();
        List<CarResponseDto> responseDtos = cars.stream()
                .map(CarMapper::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarResponseDto> updateCar(@PathVariable Long id, @Valid @RequestBody CarRequestDto carRequestDto) {
        CarModel updated = carService.update(id, carRequestDto);
        return ResponseEntity.ok(CarMapper.toResponseDto(updated));
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CarResponseDto> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(CarMapper.toResponseDto(carService.updatePhoto(id, file)));
    }

    @DeleteMapping("/{id}/photo")
    public ResponseEntity<CarResponseDto> removePhoto(@PathVariable Long id) {
        return ResponseEntity.ok(CarMapper.toResponseDto(carService.removePhoto(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCar(@PathVariable Long id) {
        carService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

