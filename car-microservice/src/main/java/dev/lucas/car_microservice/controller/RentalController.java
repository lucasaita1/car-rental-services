package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.service.RentalService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rental")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    // Endpoint para alugar um carro
    @PostMapping("/rent/{carId}/user/{userId}")
    public String rentCar(@PathVariable Long carId, @PathVariable Long userId) {
        return rentalService.rentCar(carId, userId);
    }

    // Endpoint para devolver o carro
    @PostMapping("/return/{carId}")
    public String returnCar(@PathVariable Long carId) {
        return rentalService.returnCar(carId);
    }
}
