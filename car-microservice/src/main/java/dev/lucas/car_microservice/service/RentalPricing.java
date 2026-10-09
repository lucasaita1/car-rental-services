package dev.lucas.car_microservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class RentalPricing {

    private RentalPricing() {
    }

    public static long billableDays(LocalDate start, LocalDate end) {
        // Contagem de dias corridos: o dia de início é cobrado, o dia de fim não.
        // Se as datas são iguais, cobra-se ao menos um dia.
        long days = ChronoUnit.DAYS.between(start, end);
        return days == 0 ? 1 : days;
    }

    public static BigDecimal total(BigDecimal dailyRate, LocalDate start, LocalDate end) {
        if (dailyRate == null || start == null || end == null) {
            return null;
        }
        return dailyRate.multiply(BigDecimal.valueOf(billableDays(start, end)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
