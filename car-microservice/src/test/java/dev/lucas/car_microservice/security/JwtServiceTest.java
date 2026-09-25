package dev.lucas.car_microservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(TestJwt.SECRET);

    @Test
    @DisplayName("Token válido de ADMIN vira usuário autenticado com papel ADMIN")
    void shouldReadAdminToken() {
        String token = TestJwt.token(1L, "ADMIN", Instant.now().plusSeconds(60), TestJwt.SECRET);

        AuthenticatedUser user = jwtService.verify(token).orElseThrow();

        assertThat(user.id()).isEqualTo(1L);
        assertThat(user.email()).isEqualTo("user1@email.com");
        assertThat(user.isAdmin()).isTrue();
        assertThat(user.jti()).isNotBlank();
        assertThat(user.version()).isZero();
    }

    @Test
    @DisplayName("Papel desconhecido é rebaixado para USER")
    void shouldDowngradeUnknownRole() {
        String token = TestJwt.token(1L, "SUPERUSER", Instant.now().plusSeconds(60), TestJwt.SECRET);

        assertThat(jwtService.verify(token).orElseThrow().role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("Token vencido é rejeitado")
    void shouldRejectExpiredToken() {
        String token = TestJwt.token(1L, "ADMIN", Instant.now().minusSeconds(60), TestJwt.SECRET);

        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    @DisplayName("Token sem validade é rejeitado")
    void shouldRejectTokenWithoutExpiration() {
        String token = JWT.create().withClaim("id", 1L).withClaim("role", "ADMIN")
                .sign(Algorithm.HMAC256(TestJwt.SECRET));

        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    @DisplayName("Token assinado com outro segredo é rejeitado")
    void shouldRejectForgedToken() {
        String token = TestJwt.token(1L, "ADMIN", Instant.now().plusSeconds(60), "outro-segredo");

        assertThat(jwtService.verify(token)).isEmpty();
    }

    @Test
    @DisplayName("canActFor libera o próprio usuário e o ADMIN")
    void canActForRules() {
        AuthenticatedUser user = new AuthenticatedUser(5L, "Ana", "ana@x.com", "USER", "jti", 0);
        AuthenticatedUser admin = new AuthenticatedUser(1L, "Adm", "adm@x.com", "ADMIN", "jti", 0);

        assertThat(user.canActFor(5L)).isTrue();
        assertThat(user.canActFor(6L)).isFalse();
        assertThat(admin.canActFor(6L)).isTrue();
    }

    @Test
    @DisplayName("Não sobe sem SECRET_TOKEN")
    void shouldFailWithoutSecret() {
        assertThatThrownBy(() -> new JwtService(""))
                .isInstanceOf(IllegalStateException.class);
    }
}
