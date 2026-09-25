package dev.lucas.user_microservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenConfigTest {

    private static final String SECRET = "segredo-de-teste";

    private final TokenConfig tokenConfig = new TokenConfig(SECRET);

    private UserModel usuario(UserRole role) {
        UserModel user = new UserModel();
        user.setId(7L);
        user.setName("Lucas");
        user.setEmail("lucas@email.com");
        user.setRole(role);
        return user;
    }

    @Test
    @DisplayName("Deve gravar o papel ADMIN no token e recuperá-lo na validação")
    void shouldRoundTripAdminRole() {
        String token = tokenConfig.generateToken(usuario(UserRole.ADMIN));

        JWTUserData dados = tokenConfig.verifyToken(token).orElseThrow();

        assertThat(dados.role()).isEqualTo(UserRole.ADMIN);
        assertThat(dados.isAdmin()).isTrue();
        assertThat(dados.id()).isEqualTo(7L);
        assertThat(dados.email()).isEqualTo("lucas@email.com");
    }

    @Test
    @DisplayName("Usuário comum deve sair com papel USER")
    void shouldRoundTripUserRole() {
        String token = tokenConfig.generateToken(usuario(UserRole.USER));

        assertThat(tokenConfig.verifyToken(token).orElseThrow().isAdmin()).isFalse();
    }

    @Test
    @DisplayName("Token deve ter validade de duas horas")
    void shouldExpireInTwoHours() {
        String token = tokenConfig.generateToken(usuario(UserRole.USER));

        Instant exp = JWT.decode(token).getExpiresAtAsInstant();
        Instant iat = JWT.decode(token).getIssuedAtAsInstant();

        assertThat(exp).isNotNull();
        assertThat(ChronoUnit.MINUTES.between(iat, exp)).isEqualTo(120);
    }

    @Test
    @DisplayName("Token vencido deve ser rejeitado")
    void shouldRejectExpiredToken() {
        String vencido = JWT.create()
                .withSubject("lucas@email.com")
                .withClaim("id", 7L)
                .withClaim("role", "ADMIN")
                .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRET));

        assertThat(tokenConfig.verifyToken(vencido)).isEmpty();
    }

    @Test
    @DisplayName("Token antigo, emitido sem validade, deve ser rejeitado")
    void shouldRejectTokenWithoutExpiration() {
        String eterno = JWT.create()
                .withSubject("lucas@email.com")
                .withClaim("id", 7L)
                .sign(Algorithm.HMAC256(SECRET));

        assertThat(tokenConfig.verifyToken(eterno)).isEmpty();
    }

    @Test
    @DisplayName("Token assinado com outro segredo deve ser rejeitado")
    void shouldRejectTokenSignedWithAnotherSecret() {
        String forjado = JWT.create()
                .withSubject("atacante@email.com")
                .withClaim("id", 1L)
                .withClaim("role", "ADMIN")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(Algorithm.HMAC256("outro-segredo"));

        assertThat(tokenConfig.verifyToken(forjado)).isEmpty();
    }

    @Test
    @DisplayName("Papel desconhecido no token deve ser tratado como USER")
    void shouldFallBackToUserForUnknownRole() {
        String token = JWT.create()
                .withSubject("lucas@email.com")
                .withClaim("id", 7L)
                .withClaim("role", "SUPERUSER")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(Algorithm.HMAC256(SECRET));

        assertThat(tokenConfig.verifyToken(token).orElseThrow().role()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Não deve subir sem SECRET_TOKEN configurado")
    void shouldFailWithoutSecret() {
        assertThatThrownBy(() -> new TokenConfig(" "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SECRET_TOKEN");
    }
}
