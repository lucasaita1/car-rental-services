package dev.lucas.user_microservice.seed;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAdminSeeder implements CommandLineRunner {

    static final String SEED_NAME = "default-admin";

    private final SeedRepository seedRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_NAME:GURIS PUCPR}")
    private String adminName;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        seed(adminName, adminEmail, adminPassword);
    }

    @Transactional
    public void seed(String name, String email, String password) {
        if (seedRepository.existsById(SEED_NAME)) {
            return;
        }
        if (isBlank(email) || isBlank(password)) {
            log.warn("Seeder {} ignorado: defina ADMIN_EMAIL e ADMIN_PASSWORD no .env", SEED_NAME);
            return;
        }

        String normalizedEmail = InputSanitizer.email(email);
        UserModel admin = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            UserModel user = new UserModel();
            user.setName(InputSanitizer.text(name));
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(password));
            return user;
        });
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        seedRepository.save(new SeedModel(SEED_NAME, Instant.now()));
        log.info("Seeder {} executado: administrador {} disponível", SEED_NAME, normalizedEmail);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
