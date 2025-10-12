package dev.lucas.user_microservice.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.lucas.user_microservice.entity.UserModel;
import io.github.cdimascio.dotenv.Dotenv;
import com.auth0.jwt.algorithms.Algorithm;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenConfig {

    private final String secret;

    public TokenConfig() {
        Dotenv dotenv = Dotenv.load();
        this.secret = dotenv.get("SECRET_TOKEN");
        if (this.secret == null) {
            throw new IllegalStateException("SECRET not found in .env file");
        }
    }

    public String generateToken(UserModel userModel){
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return com.auth0.jwt.JWT.create()
                .withSubject(userModel.getEmail())
                .withClaim("id", userModel.getId())
                .withClaim("name", userModel.getName())
                .sign(algorithm);
    }

    public Optional<JWTUserData> verifyToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);

            DecodedJWT jwt = JWT.require(algorithm)
                    .build()
                    .verify(token);

            return Optional.of(JWTUserData.builder()
                    .id(jwt.getClaim("id").asLong())
                    .name(jwt.getClaim("name").asString())
                    .email(jwt.getSubject())
                    .build());

        } catch (JWTVerificationException exception) {
            return Optional.empty();
        }
    }

    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}


