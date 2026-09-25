package dev.lucas.car_microservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JwtService {

    private final Algorithm algorithm;

    public JwtService(@Value("${SECRET_TOKEN}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SECRET_TOKEN não configurado");
        }
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public Optional<AuthenticatedUser> verify(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .withClaimPresence("exp")
                    .build()
                    .verify(token);

            String role = jwt.getClaim("role").asString();
            return Optional.of(new AuthenticatedUser(
                    jwt.getClaim("id").asLong(),
                    jwt.getClaim("name").asString(),
                    jwt.getSubject(),
                    "ADMIN".equals(role) ? "ADMIN" : "USER"));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }
}
