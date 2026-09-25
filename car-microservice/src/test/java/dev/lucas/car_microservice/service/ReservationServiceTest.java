package dev.lucas.car_microservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    @InjectMocks
    private ReservationService reservations;

    @Test
    @DisplayName("Carro livre é reservado atomicamente para o cliente por 10 minutos")
    void holdsFreeCar() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("car:hold:1", "5", ReservationService.HOLD_TTL)).thenReturn(true);

        assertThat(reservations.hold(1L, 5L)).isEmpty();
        verify(values).getAndSet("car:hold-user:5", "1");
    }

    @Test
    @DisplayName("Carro reservado por outro cliente devolve quem está segurando")
    void refusesCarHeldBySomeoneElse() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("car:hold:1", "5", ReservationService.HOLD_TTL)).thenReturn(false);
        when(values.get("car:hold:1")).thenReturn("9");

        assertThat(reservations.hold(1L, 5L)).contains(9L);
        verify(values, never()).getAndSet(any(), any());
    }

    @Test
    @DisplayName("Reservar de novo o próprio carro renova o prazo")
    void renewsOwnHold() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("car:hold:1", "5", ReservationService.HOLD_TTL)).thenReturn(false);
        when(values.get("car:hold:1")).thenReturn("5");

        assertThat(reservations.hold(1L, 5L)).isEmpty();
        verify(redis).expire("car:hold:1", ReservationService.HOLD_TTL);
    }

    @Test
    @DisplayName("Reservar outro carro solta o anterior do mesmo cliente")
    void switchingCarsReleasesPreviousHold() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent("car:hold:2", "5", ReservationService.HOLD_TTL)).thenReturn(true);
        when(values.getAndSet("car:hold-user:5", "2")).thenReturn("1");

        reservations.hold(2L, 5L);

        verify(redis).execute(any(RedisScript.class), eq(List.of("car:hold:1")), eq("5"));
    }

    @Test
    @DisplayName("Sem Redis a reserva falha com 503 em vez de liberar o carro")
    void holdFailsWithoutRedis() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> reservations.hold(1L, 5L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
    }

    @Test
    @DisplayName("Soltar só apaga a reserva se ela ainda for do mesmo cliente")
    void releaseUsesCompareAndDelete() {
        reservations.release(1L, 5L);

        verify(redis).execute(any(RedisScript.class), eq(List.of("car:hold:1")), eq("5"));
        verify(redis).execute(any(RedisScript.class), eq(List.of("car:hold-user:5")), eq("1"));
    }

    @Test
    @DisplayName("Catálogo identifica os carros reservados")
    void listsHeldCars() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.multiGet(List.of("car:hold:1", "car:hold:2", "car:hold:3")))
                .thenReturn(Arrays.asList("5", null, "8"));

        assertThat(reservations.heldCarIds(List.of(1L, 2L, 3L))).containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("Catálogo continua funcionando sem Redis")
    void heldCarsFailOpen() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(reservations.heldCarIds(List.of(1L))).isEmpty();
        assertThat(reservations.heldCarIds(List.of())).isEmpty();
    }

    @Test
    @DisplayName("Informa quando a reserva expira")
    void reportsExpiry() {
        when(redis.getExpire("car:hold:1")).thenReturn(300L);

        assertThat(reservations.expiresAt(1L)).isBetween(Instant.now().plusSeconds(295), Instant.now().plusSeconds(301));
    }
}
