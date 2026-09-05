package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O AuthService é o ponto que o Spring Security chama durante a autenticação.
 * A regra que importa aqui é que a busca acontece por e-mail, e não pelo campo
 * username do UserDetails, que nesta entidade está vazio.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Deve localizar o usuário pelo e-mail informado no login")
    void shouldLoadUserByEmail() {
        UserModel user = new UserModel();
        user.setId(1L);
        user.setName("Lucas");
        user.setEmail("lucas@email.com");
        user.setPassword("$2a$10$hash");
        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(user));

        UserDetails encontrado = authService.loadUserByUsername("lucas@email.com");

        assertThat(encontrado).isSameAs(user);
        assertThat(encontrado.getPassword()).isEqualTo("$2a$10$hash");
        // O parâmetro do Spring Security se chama "username", mas a consulta é por e-mail.
        verify(userRepository).findByEmail("lucas@email.com");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException para e-mail inexistente")
    void shouldThrowWhenEmailNotRegistered() {
        when(userRepository.findByEmail("naoexiste@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("naoexiste@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("A mensagem de erro não deve revelar se o e-mail existe na base")
    void shouldNotLeakAccountExistence() {
        when(userRepository.findByEmail("alvo@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loadUserByUsername("alvo@email.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                // Enumeração de contas: a mensagem trata e-mail e senha em conjunto,
                // sem confirmar a um atacante que o endereço está cadastrado.
                .hasMessageContaining("Email or password inaccurate")
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("alvo@email.com"));
    }

    @Test
    @DisplayName("A conta carregada deve estar ativa e sem restrições")
    void shouldReturnEnabledAccount() {
        UserModel user = new UserModel();
        user.setEmail("lucas@email.com");
        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(user));

        UserDetails encontrado = authService.loadUserByUsername("lucas@email.com");

        assertThat(encontrado.isEnabled()).isTrue();
        assertThat(encontrado.isAccountNonExpired()).isTrue();
        assertThat(encontrado.isAccountNonLocked()).isTrue();
        assertThat(encontrado.isCredentialsNonExpired()).isTrue();
    }
}
