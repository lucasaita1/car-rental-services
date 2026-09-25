package dev.lucas.car_microservice.dto;

import java.time.Instant;

public record HoldResponse(Long carId, Instant expiresAt) {
}
