package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.producer.UserProducer;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.security.TokenRevocationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class PasswordResetService {

    static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final String TOKEN_PREFIX = "auth:pwd-reset:";
    static final String USER_PREFIX = "auth:pwd-reset-user:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redis;
    private final UserProducer userProducer;
    private final TokenRevocationService tokenRevocationService;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                StringRedisTemplate redis,
                                UserProducer userProducer,
                                TokenRevocationService tokenRevocationService,
                                @Value("${FRONTEND_URL:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.redis = redis;
        this.userProducer = userProducer;
        this.tokenRevocationService = tokenRevocationService;
        this.frontendUrl = frontendUrl.split(",")[0].trim().replaceAll("/+$", "");
    }

    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = newToken();
            String hash = sha256(token);

            String previous = redis.opsForValue().get(USER_PREFIX + user.getId());
            if (previous != null) {
                redis.delete(TOKEN_PREFIX + previous);
            }
            redis.opsForValue().set(TOKEN_PREFIX + hash, user.getId().toString(), TOKEN_TTL);
            redis.opsForValue().set(USER_PREFIX + user.getId(), hash, TOKEN_TTL);

            userProducer.sendPasswordResetEmail(user, frontendUrl + "/redefinir-senha?token=" + token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String userId = redis.opsForValue().getAndDelete(TOKEN_PREFIX + sha256(token));
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado.");
        }

        UserModel user = userRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redis.delete(USER_PREFIX + userId);
        tokenRevocationService.revokeAll(user.getId());
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
