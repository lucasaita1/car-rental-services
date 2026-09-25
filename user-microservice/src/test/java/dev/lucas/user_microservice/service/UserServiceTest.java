package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.dtos.ProfileUpdateRequest;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.producer.UserProducer;
import dev.lucas.user_microservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O UserService concentra a regra mais sensível do sistema: nenhuma senha pode
 * chegar ao banco em texto puro. Também é ele quem dispara o evento de
 * boas-vindas, então a ordem das operações importa.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserProducer userProducer;

    @InjectMocks
    private UserService userService;

    private UserModel novoUsuario;

    @BeforeEach
    void setUp() {
        novoUsuario = new UserModel();
        novoUsuario.setName("Lucas Aita");
        novoUsuario.setEmail("lucas@email.com");
        novoUsuario.setCpf("12345678900");
        novoUsuario.setCnh("98765432100");
        novoUsuario.setPassword("senhaEmTextoPuro");
    }

    @Test
    @DisplayName("Nunca deve persistir a senha em texto puro")
    void shouldNeverPersistPlainTextPassword() {
        when(passwordEncoder.encode("senhaEmTextoPuro")).thenReturn("$2a$10$hashBCrypt");
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        userService.saveUser(novoUsuario);

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userRepository).save(captor.capture());

        assertThat(captor.getValue().getPassword())
                .isEqualTo("$2a$10$hashBCrypt")
                .isNotEqualTo("senhaEmTextoPuro");
    }

    @Test
    @DisplayName("Deve publicar o evento de boas-vindas ao cadastrar")
    void shouldPublishWelcomeEvent() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        userService.saveUser(novoUsuario);

        verify(userProducer).sendRegisterEmail(novoUsuario);
    }

    @Test
    @DisplayName("Deve criptografar, depois salvar e só então publicar o evento")
    void shouldEncodeBeforeAnySideEffect() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        userService.saveUser(novoUsuario);

        // A criptografia vem primeiro: o objeto é o mesmo passado adiante, então
        // codificar depois deixaria a senha crua vazar para a fila. E o evento
        // só pode sair após o save, quando o id já foi gerado pelo banco.
        InOrder ordem = org.mockito.Mockito.inOrder(passwordEncoder, userRepository, userProducer);
        ordem.verify(passwordEncoder).encode("senhaEmTextoPuro");
        ordem.verify(userRepository).save(any(UserModel.class));
        ordem.verify(userProducer).sendRegisterEmail(any(UserModel.class));
    }

    @Test
    @DisplayName("Deve publicar o evento somente após o banco gerar o id")
    void shouldPublishEventWithGeneratedId() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(userRepository.save(any(UserModel.class))).thenAnswer(invocation -> {
            UserModel u = invocation.getArgument(0);
            u.setId(1L); // o id só existe depois que o banco grava
            return u;
        });

        userService.saveUser(novoUsuario);

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userProducer).sendRegisterEmail(captor.capture());

        // Sem isso o EmailDto sairia com userId nulo e o histórico no MongoDB
        // ficaria sem vínculo com o usuário que originou a mensagem.
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve devolver a lista de usuários do repositório")
    void shouldListAllUsers() {
        UserModel outro = new UserModel();
        outro.setName("Rafael");
        when(userRepository.findAll()).thenReturn(List.of(novoUsuario, outro));

        assertThat(userService.getAllUsers()).hasSize(2);
        verify(userRepository).findAll();
    }

    @Test
    @DisplayName("Deve devolver Optional vazio quando o usuário não existe")
    void shouldReturnEmptyWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.getUserById(99L)).isEmpty();
    }

    @Test
    @DisplayName("Cadastro com e-mail já usado retorna 409")
    void shouldRejectDuplicateEmailOnRegister() {
        when(userRepository.existsByEmail("lucas@email.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.saveUser(novoUsuario))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(userRepository, never()).save(any());
        verify(userProducer, never()).sendRegisterEmail(any());
    }

    @Test
    @DisplayName("Atualização de perfil troca nome, e-mail, CPF e CNH")
    void shouldUpdateProfile() {
        UserModel existente = new UserModel();
        existente.setId(1L);
        existente.setPassword("hashAntigo");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(userRepository.save(any(UserModel.class))).thenAnswer(i -> i.getArgument(0));

        UserModel atualizado = userService.updateProfile(1L,
                new ProfileUpdateRequest("Nome Novo", "novo@email.com", "12345678900", "98765432100")).orElseThrow();

        assertThat(atualizado.getName()).isEqualTo("Nome Novo");
        assertThat(atualizado.getEmail()).isEqualTo("novo@email.com");
        assertThat(atualizado.getCpf()).isEqualTo("12345678900");
        assertThat(atualizado.getCnh()).isEqualTo("98765432100");
        assertThat(atualizado.getPassword()).isEqualTo("hashAntigo");
    }

    @Test
    @DisplayName("Atualização para e-mail de outra conta retorna 409")
    void shouldRejectEmailOwnedByAnotherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new UserModel()));
        when(userRepository.existsByEmailAndIdNot("outro@email.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(1L,
                new ProfileUpdateRequest("Nome", "outro@email.com", null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Atualização de id inexistente deve devolver Optional vazio")
    void shouldReturnEmptyWhenUpdatingUnknownId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.updateProfile(99L,
                new ProfileUpdateRequest("Nome", "a@b.com", null, null))).isEmpty();
    }

    @Test
    @DisplayName("Deve delegar a remoção ao repositório")
    void shouldDeleteById() {
        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }
}
