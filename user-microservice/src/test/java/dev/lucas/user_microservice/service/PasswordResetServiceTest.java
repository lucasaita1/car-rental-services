package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.producer.UserProducer;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.security.TokenRevocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> values;

    @Mock
    private UserProducer userProducer;

    @Mock
    private TokenRevocationService tokenRevocationService;

    private PasswordResetService service;

    private UserModel ana;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, passwordEncoder, redis, userProducer,
                tokenRevocationService, "http://localhost:5173/, http://outro");
        ana = new UserModel();
        ana.setId(7L);
        ana.setName("Ana");
        ana.setEmail("ana@x.com");
    }

    @Test
    @DisplayName("Pedido para e-mail cadastrado envia link com token e guarda só o hash")
    void requestSendsLinkAndStoresHash() {
        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));
        when(redis.opsForValue()).thenReturn(values);

        service.requestReset("ana@x.com");

        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(userProducer).sendPasswordResetEmail(eq(ana), link.capture());
        assertThat(link.getValue()).startsWith("http://localhost:5173/redefinir-senha?token=");

        String token = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        verify(values).set(eq("auth:pwd-reset:" + PasswordResetService.sha256(token)), eq("7"), eq(PasswordResetService.TOKEN_TTL));
        verify(values, never()).set(eq("auth:pwd-reset:" + token), anyString(), any());
    }

    @Test
    @DisplayName("Pedido para e-mail desconhecido não faz nada nem revela a ausência")
    void requestForUnknownEmailDoesNothing() {
        when(userRepository.findByEmail("ninguem@x.com")).thenReturn(Optional.empty());

        service.requestReset("ninguem@x.com");

        verifyNoInteractions(userProducer, redis);
    }

    @Test
    @DisplayName("Novo pedido invalida o link anterior")
    void newRequestInvalidatesPreviousLink() {
        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(ana));
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:pwd-reset-user:7")).thenReturn("hashAntigo");

        service.requestReset("ana@x.com");

        verify(redis).delete("auth:pwd-reset:hashAntigo");
    }

    @Test
    @DisplayName("Link válido troca a senha, consome o token e derruba as sessões")
    void resetChangesPasswordAndRevokesSessions() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete("auth:pwd-reset:" + PasswordResetService.sha256("tok"))).thenReturn("7");
        when(userRepository.findById(7L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.encode("novaSenha123")).thenReturn("$2a$novo");

        service.resetPassword("tok", "novaSenha123");

        assertThat(ana.getPassword()).isEqualTo("$2a$novo");
        verify(userRepository).save(ana);
        verify(redis).delete("auth:pwd-reset-user:7");
        verify(tokenRevocationService).revokeAll(7L);
    }

    @Test
    @DisplayName("Link inválido, expirado ou já usado retorna 400")
    void invalidTokenIsRejected() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.getAndDelete(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword("tok", "novaSenha123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Link inválido ou expirado");
        verify(userRepository, never()).save(any());
    }
}
