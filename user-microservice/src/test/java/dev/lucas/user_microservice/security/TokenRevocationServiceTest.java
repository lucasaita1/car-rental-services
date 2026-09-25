package dev.lucas.user_microservice.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevocationServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    @InjectMocks
    private TokenRevocationService service;

    @Test
    @DisplayName("Logout grava o jti com TTL igual ao tempo que resta do token")
    void revokeStoresJtiUntilExpiry() {
        when(redis.opsForValue()).thenReturn(values);

        service.revoke("abc", Instant.now().plusSeconds(600));

        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);
        verify(values).set(eq("auth:revoked:abc"), eq("1"), ttl.capture());
        assertThat(ttl.getValue()).isBetween(Duration.ofSeconds(590), Duration.ofSeconds(600));
    }

    @Test
    @DisplayName("Token já vencido não precisa ir para a lista")
    void revokeIgnoresExpiredToken() {
        service.revoke("abc", Instant.now().minusSeconds(10));

        verifyNoInteractions(redis);
    }

    @Test
    @DisplayName("revokeAll incrementa a versão do usuário")
    void revokeAllBumpsVersion() {
        when(redis.opsForValue()).thenReturn(values);

        service.revokeAll(7L);

        verify(values).increment("auth:token-version:7");
    }

    @Test
    @DisplayName("Versão inexistente vale zero")
    void missingVersionIsZero() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:7")).thenReturn(null);

        assertThat(service.currentVersion(7L)).isZero();
    }

    @Test
    @DisplayName("Sem Redis o login falha com 503 em vez de emitir token sem controle")
    void currentVersionFailsWhenRedisIsDown() {
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> service.currentVersion(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
    }

    @Test
    @DisplayName("Token com jti na lista está revogado")
    void revokedJti() {
        when(redis.hasKey("auth:revoked:abc")).thenReturn(true);

        assertThat(service.isRevoked("abc", 7L, 0)).isTrue();
    }

    @Test
    @DisplayName("Token de versão anterior está revogado")
    void outdatedVersion() {
        when(redis.hasKey("auth:revoked:abc")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:7")).thenReturn("1");

        assertThat(service.isRevoked("abc", 7L, 0)).isTrue();
        assertThat(service.isRevoked("abc", 7L, 1)).isFalse();
    }

    @Test
    @DisplayName("Token sem jti é rejeitado sem consultar o Redis")
    void missingJtiIsRevoked() {
        assertThat(service.isRevoked(null, 7L, 0)).isTrue();
        verify(redis, never()).hasKey(any());
    }

    @Test
    @DisplayName("Redis fora do ar bloqueia o token")
    void failsClosed() {
        when(redis.hasKey("auth:revoked:abc")).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(service.isRevoked("abc", 7L, 0)).isTrue();
    }
}
