package dev.lucas.car_microservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RentalPricingTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 9, 26);

    @Test
    @DisplayName("Cobra uma diária por dia corrido")
    void shouldChargePerDay() {
        assertThat(RentalPricing.billableDays(INICIO, INICIO.plusDays(3))).isEqualTo(3);
        assertThat(RentalPricing.total(new BigDecimal("149.90"), INICIO, INICIO.plusDays(3)))
                .isEqualByComparingTo("449.70");
    }

    @Test
    @DisplayName("Devolução no mesmo dia cobra uma diária mínima")
    void shouldChargeAtLeastOneDay() {
        assertThat(RentalPricing.billableDays(INICIO, INICIO)).isEqualTo(1);
        assertThat(RentalPricing.total(new BigDecimal("100"), INICIO, INICIO)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Atravessa virada de mês sem erro")
    void shouldCrossMonthBoundary() {
        assertThat(RentalPricing.billableDays(LocalDate.of(2026, 1, 30), LocalDate.of(2026, 2, 2))).isEqualTo(3);
    }

    @Test
    @DisplayName("Sem diária ou sem data final, não há total")
    void shouldReturnNullWhenIncomplete() {
        assertThat(RentalPricing.total(null, INICIO, INICIO.plusDays(1))).isNull();
        assertThat(RentalPricing.total(BigDecimal.TEN, INICIO, null)).isNull();
    }

    @Test
    @DisplayName("Arredonda para centavos")
    void shouldRoundToCents() {
        assertThat(RentalPricing.total(new BigDecimal("33.333"), INICIO, INICIO.plusDays(3)).scale()).isEqualTo(2);
    }
}
