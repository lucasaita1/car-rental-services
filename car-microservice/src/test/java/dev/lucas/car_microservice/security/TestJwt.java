package dev.lucas.car_microservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.Date;

public final class TestJwt {

    public static final String SECRET = "segredo-de-teste";

    private TestJwt() {
    }

    public static String token(Long id, String role, Instant expiresAt, String secret) {
        return JWT.create()
                .withSubject("user" + id + "@email.com")
                .withClaim("id", id)
                .withClaim("name", "Usuário " + id)
                .withClaim("role", role)
                .withExpiresAt(Date.from(expiresAt))
                .sign(Algorithm.HMAC256(secret));
    }

    public static String bearer(Long id, String role) {
        return "Bearer " + token(id, role, Instant.now().plusSeconds(3600), SECRET);
    }

    public static String admin() {
        return bearer(1L, "ADMIN");
    }

    public static String user(Long id) {
        return bearer(id, "USER");
    }

    @TestConfiguration
    public static class Config {
        @Bean
        JwtService jwtService() {
            return new JwtService(SECRET);
        }
    }
}
