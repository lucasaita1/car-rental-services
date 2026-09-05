package dev.lucas.user_microservice.service;

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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Atualização deve ignorar campos nulos ou em branco")
    void shouldIgnoreBlankFieldsOnUpdate() {
        UserModel existente = new UserModel();
        existente.setId(1L);
        existente.setName("Nome Antigo");
        existente.setEmail("antigo@email.com");
        existente.setPassword("hashAntigo");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existente));

        UserModel alteracoes = new UserModel();
        alteracoes.setName("Nome Novo");
        alteracoes.setEmail("   ");   // em branco: deve ser ignorado
        alteracoes.setPassword(null); // nulo: deve ser ignorado

        UserModel atualizado = userService.updateById(1L, alteracoes).orElseThrow();

        assertThat(atualizado.getName()).isEqualTo("Nome Novo");
        assertThat(atualizado.getEmail()).isEqualTo("antigo@email.com");
        assertThat(atualizado.getPassword()).isEqualTo("hashAntigo");
    }

    @Test
    @DisplayName("Deve criptografar também a senha trocada na atualização")
    void shouldEncodePasswordOnUpdate() {
        UserModel existente = new UserModel();
        existente.setId(1L);
        existente.setPassword("hashAntigo");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(passwordEncoder.encode("novaSenhaEmTextoPuro")).thenReturn("$2a$10$novoHash");

        UserModel alteracoes = new UserModel();
        alteracoes.setPassword("novaSenhaEmTextoPuro");

        UserModel atualizado = userService.updateById(1L, alteracoes).orElseThrow();

        // Gravar a senha crua aqui impediria o login, porque o BCrypt do
        // AuthenticationManager nunca casaria com um valor não criptografado.
        assertThat(atualizado.getPassword())
                .isEqualTo("$2a$10$novoHash")
                .isNotEqualTo("novaSenhaEmTextoPuro");
        verify(passwordEncoder).encode("novaSenhaEmTextoPuro");
    }

    @Test
    @DisplayName("Atualização de id inexistente deve devolver Optional vazio")
    void shouldReturnEmptyWhenUpdatingUnknownId() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.updateById(99L, new UserModel())).isEmpty();
    }

    @Test
    @DisplayName("Deve delegar a remoção ao repositório")
    void shouldDeleteById() {
        userService.deleteById(1L);

        verify(userRepository).deleteById(1L);
    }
}
