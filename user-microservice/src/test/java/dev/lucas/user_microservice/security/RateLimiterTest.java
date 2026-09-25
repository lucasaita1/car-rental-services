package dev.lucas.user_microservice.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    private static final Duration MINUTE = Duration.ofMinutes(1);

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    @InjectMocks
    private RateLimiter rateLimiter;

    @Test
    @DisplayName("Primeira tentativa abre a janela com expiração")
    void firstHitStartsWindow() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment("rl:login:1.2.3.4")).thenReturn(1L);

        assertThat(rateLimiter.retryAfterSeconds("login:1.2.3.4", 5, MINUTE)).isZero();
        verify(redis).expire("rl:login:1.2.3.4", MINUTE);
    }

    @Test
    @DisplayName("Tentativas dentro do limite passam sem renovar a janela")
    void hitsWithinLimitPass() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment("rl:login:1.2.3.4")).thenReturn(5L);

        assertThat(rateLimiter.retryAfterSeconds("login:1.2.3.4", 5, MINUTE)).isZero();
        verify(redis, never()).expire(any(), any(Duration.class));
    }

    @Test
    @DisplayName("Acima do limite devolve o tempo restante da janela")
    void overLimitReturnsRemainingTtl() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment("rl:login:1.2.3.4")).thenReturn(6L);
        when(redis.getExpire("rl:login:1.2.3.4")).thenReturn(37L);

        assertThat(rateLimiter.retryAfterSeconds("login:1.2.3.4", 5, MINUTE)).isEqualTo(37);
    }

    @Test
    @DisplayName("Chave sem expiração é corrigida para não bloquear para sempre")
    void keyWithoutTtlIsRepaired() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.increment("rl:login:1.2.3.4")).thenReturn(9L);
        when(redis.getExpire("rl:login:1.2.3.4")).thenReturn(-1L);

        assertThat(rateLimiter.retryAfterSeconds("login:1.2.3.4", 5, MINUTE)).isEqualTo(60);
        verify(redis).expire("rl:login:1.2.3.4", MINUTE);
    }

    @Test
    @DisplayName("Redis fora do ar não impede o login")
    void failsOpenWhenRedisIsDown() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(rateLimiter.retryAfterSeconds("login:1.2.3.4", 5, MINUTE)).isZero();
    }
}
