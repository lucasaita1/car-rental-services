package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.dto.CarResponseDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.mapper.CarMapper;
import dev.lucas.car_microservice.service.CarService;
import dev.lucas.car_microservice.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cars")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final ReservationService reservationService;

    // Laboratório Aula 4 (canary): pausa que cresce 3 ms a cada requisição,
    // somente quando a versão sobe com SIMULAR_LENTIDAO=1. Em produção o
    // ambiente não define a variável e este método não faz nada.
    private static final AtomicInteger CONTADOR_LENTIDAO = new AtomicInteger();

    private static void simularLentidao() {
        if ("1".equals(System.getenv("SIMULAR_LENTIDAO"))) {
            try {
                Thread.sleep(40 + 3L * CONTADOR_LENTIDAO.incrementAndGet());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

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
        responseDto.setReserved(!reservationService.heldCarIds(List.of(id)).isEmpty());
        return ResponseEntity.ok(responseDto);
    }

    @GetMapping
    public ResponseEntity<List<CarResponseDto>> getAllCars() {
        simularLentidao();
        List<CarModel> cars = carService.findAll();
        Set<Long> held = reservationService.heldCarIds(cars.stream().map(CarModel::getId).toList());
        List<CarResponseDto> responseDtos = cars.stream()
                .map(CarMapper::toResponseDto)
                .peek(dto -> dto.setReserved(held.contains(dto.getId())))
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

