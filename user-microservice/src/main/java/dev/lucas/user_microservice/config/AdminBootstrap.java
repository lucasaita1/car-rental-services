package dev.lucas.user_microservice.config;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${ADMIN_NAME:Administrador}")
    private String adminName;

    @Override
    public void run(String... args) {
        ensureAdmin(adminEmail, adminPassword, adminName);
    }

    @Transactional
    public void ensureAdmin(String email, String password, String name) {
        if (isBlank(email) || isBlank(password)) {
            return;
        }

        userRepository.findByEmail(email).ifPresentOrElse(
                existing -> {
                    if (!existing.isAdmin()) {
                        existing.setRole(UserRole.ADMIN);
                        userRepository.save(existing);
                    }
                },
                () -> {
                    UserModel admin = new UserModel();
                    admin.setName(name);
                    admin.setEmail(email);
                    admin.setPassword(passwordEncoder.encode(password));
                    admin.setRole(UserRole.ADMIN);
                    userRepository.save(admin);
                });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
