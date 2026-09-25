package dev.lucas.user_microservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenConfig {

    static final Duration TOKEN_TTL = Duration.ofHours(2);

    private final String secret;

    public TokenConfig(@Value("${SECRET_TOKEN}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("SECRET_TOKEN não configurado");
        }
        this.secret = secret;
    }

    public String generateToken(UserModel userModel, long version){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        Instant now = Instant.now();
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(userModel.getEmail())
                .withClaim("id", userModel.getId())
                .withClaim("name", userModel.getName())
                .withClaim("role", userModel.getRole().name())
                .withClaim("ver", version)
                .withIssuedAt(now)
                .withExpiresAt(now.plus(TOKEN_TTL))
                .sign(algorithm);
    }

    public Optional<JWTUserData> verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            DecodedJWT jwt = JWT.require(algorithm)
                    .withClaimPresence("exp")
                    .build()
                    .verify(token);

            return Optional.of(JWTUserData.builder()
                    .id(jwt.getClaim("id").asLong())
                    .name(jwt.getClaim("name").asString())
                    .email(jwt.getSubject())
                    .role(parseRole(jwt.getClaim("role").asString()))
                    .jti(jwt.getId())
                    .version(jwt.getClaim("ver").isMissing() ? 0 : jwt.getClaim("ver").asLong())
                    .expiresAt(jwt.getExpiresAtAsInstant())
                    .build());

        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    private static UserRole parseRole(String role) {
        try {
            return role == null ? UserRole.USER : UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return UserRole.USER;
        }
    }
}
