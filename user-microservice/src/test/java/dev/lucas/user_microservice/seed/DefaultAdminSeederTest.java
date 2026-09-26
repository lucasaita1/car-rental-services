package dev.lucas.user_microservice.seed;

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
class DefaultAdminSeederTest {

    @Mock
    private SeedRepository seedRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DefaultAdminSeeder seeder;

    @Test
    @DisplayName("Na primeira execução deve criar o administrador e registrar o seed")
    void shouldCreateAdminOnFirstRun() {
        when(seedRepository.existsById(DefaultAdminSeeder.SEED_NAME)).thenReturn(false);
        when(userRepository.findByEmail("admin@pucpr.edu.br")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("SenhaForte@1")).thenReturn("$2a$10$hash");

        seeder.seed("GURIS PUCPR", " Admin@PUCPR.edu.br ", "SenhaForte@1");

        ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        verify(userRepository).save(userCaptor.capture());
        UserModel admin = userCaptor.getValue();
        assertThat(admin.getName()).isEqualTo("GURIS PUCPR");
        assertThat(admin.getEmail()).isEqualTo("admin@pucpr.edu.br");
        assertThat(admin.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);

        ArgumentCaptor<SeedModel> seedCaptor = ArgumentCaptor.forClass(SeedModel.class);
        verify(seedRepository).save(seedCaptor.capture());
        assertThat(seedCaptor.getValue().getName()).isEqualTo(DefaultAdminSeeder.SEED_NAME);
    }

    @Test
    @DisplayName("Se o seed já rodou, não deve criar nada, mesmo que o admin tenha sido removido")
    void shouldSkipWhenAlreadyExecuted() {
        when(seedRepository.existsById(DefaultAdminSeeder.SEED_NAME)).thenReturn(true);

        seeder.seed("GURIS PUCPR", "admin@pucpr.edu.br", "SenhaForte@1");

        verifyNoInteractions(userRepository, passwordEncoder);
        verify(seedRepository, never()).save(any());
    }

    @Test
    @DisplayName("Conta já existente com o mesmo e-mail é promovida sem trocar a senha")
    void shouldPromoteExistingAccountKeepingPassword() {
        UserModel existente = new UserModel();
        existente.setEmail("admin@pucpr.edu.br");
        existente.setPassword("$2a$10$original");
        when(seedRepository.existsById(DefaultAdminSeeder.SEED_NAME)).thenReturn(false);
        when(userRepository.findByEmail("admin@pucpr.edu.br")).thenReturn(Optional.of(existente));

        seeder.seed("GURIS PUCPR", "admin@pucpr.edu.br", "OutraSenha@1");

        assertThat(existente.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(existente.getPassword()).isEqualTo("$2a$10$original");
        verify(passwordEncoder, never()).encode(any());
        verify(seedRepository).save(any(SeedModel.class));
    }

    @Test
    @DisplayName("Sem credenciais configuradas não cria nem marca o seed como executado")
    void shouldNotMarkSeedWithoutCredentials() {
        when(seedRepository.existsById(DefaultAdminSeeder.SEED_NAME)).thenReturn(false);

        seeder.seed("GURIS PUCPR", "admin@pucpr.edu.br", " ");

        verifyNoInteractions(userRepository, passwordEncoder);
        verify(seedRepository, never()).save(any());
    }
}
