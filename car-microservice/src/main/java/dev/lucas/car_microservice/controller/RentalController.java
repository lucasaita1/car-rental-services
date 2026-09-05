package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.service.RentalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rental")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    /**
     * Aluga um veículo. O prazo de devolução é opcional; quando informado,
     * alimenta a consulta de locações vencidas.
     */
    @PostMapping("/rent/{carId}/user/{userId}")
    public String rentCar(
            @PathVariable Long carId,
            @PathVariable Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedReturnDate) {
        return rentalService.rentCar(carId, userId, expectedReturnDate);
    }

    // Endpoint para devolver o carro
    @PostMapping("/return/{carId}")
    public String returnCar(@PathVariable Long carId) {
        return rentalService.returnCar(carId);
    }

    /** Histórico de locações de um cliente. */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RentalResponseDto>> rentalsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(rentalService.findRentalsByUser(userId));
    }

    /** Histórico de locações de um veículo. */
    @GetMapping("/car/{carId}")
    public ResponseEntity<List<RentalResponseDto>> rentalsByCar(@PathVariable Long carId) {
        return ResponseEntity.ok(rentalService.findRentalsByCar(carId));
    }

    /** Locações em aberto. */
    @GetMapping("/active")
    public ResponseEntity<List<RentalResponseDto>> activeRentals() {
        return ResponseEntity.ok(rentalService.findActiveRentals());
    }

    /** Locações ativas com o prazo de devolução vencido. */
    @GetMapping("/overdue")
    public ResponseEntity<List<RentalResponseDto>> overdueRentals() {
        return ResponseEntity.ok(rentalService.findOverdueRentals());
    }
}
