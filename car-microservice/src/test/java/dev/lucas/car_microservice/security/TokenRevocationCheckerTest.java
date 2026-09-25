package dev.lucas.car_microservice.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRevocationCheckerTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    @InjectMocks
    private TokenRevocationChecker checker;

    private AuthenticatedUser user(String jti, long version) {
        return new AuthenticatedUser(5L, "Ana", "ana@x.com", "USER", jti, version);
    }

    @Test
    @DisplayName("Token ativo não é revogado")
    void activeTokenIsNotRevoked() {
        when(redis.hasKey("auth:revoked:abc")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:5")).thenReturn(null);

        assertThat(checker.isRevoked(user("abc", 0))).isFalse();
    }

    @Test
    @DisplayName("Token que fez logout é revogado")
    void loggedOutTokenIsRevoked() {
        when(redis.hasKey("auth:revoked:abc")).thenReturn(true);

        assertThat(checker.isRevoked(user("abc", 0))).isTrue();
    }

    @Test
    @DisplayName("Token de versão antiga é revogado após troca de papel ou senha")
    void outdatedVersionIsRevoked() {
        when(redis.hasKey("auth:revoked:abc")).thenReturn(false);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:token-version:5")).thenReturn("2");

        assertThat(checker.isRevoked(user("abc", 1))).isTrue();
        assertThat(checker.isRevoked(user("abc", 2))).isFalse();
    }

    @Test
    @DisplayName("Token sem jti é rejeitado")
    void tokenWithoutJtiIsRevoked() {
        assertThat(checker.isRevoked(user(null, 0))).isTrue();
    }

    @Test
    @DisplayName("Redis fora do ar bloqueia o token em vez de liberar")
    void failsClosedWhenRedisIsDown() {
        when(redis.hasKey("auth:revoked:abc")).thenThrow(new RedisConnectionFailureException("down"));

        assertThat(checker.isRevoked(user("abc", 0))).isTrue();
    }
}
