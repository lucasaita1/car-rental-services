package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.dto.HoldResponse;
import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.security.AuthenticatedUser;
import dev.lucas.car_microservice.service.RentalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/rental")
public class RentalController {

    private final RentalService rentalService;

    public RentalController(RentalService rentalService) {
        this.rentalService = rentalService;
    }

    @PostMapping("/rent/{carId}/user/{userId}")
    public String rentCar(
            @PathVariable Long carId,
            @PathVariable Long userId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedReturnDate,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        requireCanActFor(principal, userId);
        return rentalService.rentCar(carId, userId, expectedReturnDate);
    }

    @PostMapping("/hold/{carId}")
    public HoldResponse holdCar(@PathVariable Long carId,
                                @AuthenticationPrincipal AuthenticatedUser principal) {
        return rentalService.holdCar(carId, principal.id());
    }

    @DeleteMapping("/hold/{carId}")
    public ResponseEntity<Void> releaseHold(@PathVariable Long carId,
                                            @AuthenticationPrincipal AuthenticatedUser principal) {
        rentalService.releaseHold(carId, principal.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/return/{carId}")
    public String returnCar(@PathVariable Long carId,
                            @AuthenticationPrincipal AuthenticatedUser principal) {
        rentalService.findActiveRenterId(carId)
                .ifPresent(renterId -> requireCanActFor(principal, renterId));
        return rentalService.returnCar(carId);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RentalResponseDto>> rentalsByUser(@PathVariable Long userId,
                                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        requireCanActFor(principal, userId);
        return ResponseEntity.ok(rentalService.findRentalsByUser(userId));
    }

    @GetMapping("/car/{carId}")
    public ResponseEntity<List<RentalResponseDto>> rentalsByCar(@PathVariable Long carId) {
        return ResponseEntity.ok(rentalService.findRentalsByCar(carId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<RentalResponseDto>> activeRentals() {
        return ResponseEntity.ok(rentalService.findActiveRentals());
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<RentalResponseDto>> overdueRentals() {
        return ResponseEntity.ok(rentalService.findOverdueRentals());
    }

    private void requireCanActFor(AuthenticatedUser principal, Long userId) {
        if (!principal.canActFor(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode operar locações da sua própria conta.");
        }
    }
}
