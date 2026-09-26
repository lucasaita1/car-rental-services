package dev.lucas.car_microservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class RentalPricing {

    private RentalPricing() {
    }

    public static long billableDays(LocalDate start, LocalDate end) {
        return Math.max(1, ChronoUnit.DAYS.between(start, end));
    }

    public static BigDecimal total(BigDecimal dailyRate, LocalDate start, LocalDate end) {
        if (dailyRate == null || start == null || end == null) {
            return null;
        }
        return dailyRate.multiply(BigDecimal.valueOf(billableDays(start, end)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
