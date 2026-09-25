package dev.lucas.user_microservice.config;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminBootstrap adminBootstrap;

    @Test
    @DisplayName("Deve criar o administrador com senha criptografada quando não existe")
    void shouldCreateAdminWhenMissing() {
        when(userRepository.findByEmail("admin@carrental.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("senhaForte")).thenReturn("$2a$10$hash");

        adminBootstrap.ensureAdmin("admin@carrental.local", "senhaForte", "Administrador");

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userRepository).save(captor.capture());

        UserModel admin = captor.getValue();
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@carrental.local");
        assertThat(admin.getPassword()).isEqualTo("$2a$10$hash");
    }

    @Test
    @DisplayName("Deve promover conta existente sem trocar a senha dela")
    void shouldPromoteExistingAccountKeepingPassword() {
        UserModel existente = new UserModel();
        existente.setEmail("admin@carrental.local");
        existente.setPassword("$2a$10$senhaOriginal");
        existente.setRole(UserRole.USER);
        when(userRepository.findByEmail("admin@carrental.local")).thenReturn(Optional.of(existente));

        adminBootstrap.ensureAdmin("admin@carrental.local", "outraSenha", "Administrador");

        assertThat(existente.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(existente.getPassword()).isEqualTo("$2a$10$senhaOriginal");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("Não deve regravar quem já é administrador")
    void shouldSkipWhenAlreadyAdmin() {
        UserModel admin = new UserModel();
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail("admin@carrental.local")).thenReturn(Optional.of(admin));

        adminBootstrap.ensureAdmin("admin@carrental.local", "senha", "Administrador");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Sem ADMIN_EMAIL ou ADMIN_PASSWORD configurados, não deve fazer nada")
    void shouldDoNothingWithoutCredentials() {
        adminBootstrap.ensureAdmin(null, "senha", "Administrador");
        adminBootstrap.ensureAdmin("admin@carrental.local", " ", "Administrador");

        verifyNoInteractions(userRepository, passwordEncoder);
    }
}
