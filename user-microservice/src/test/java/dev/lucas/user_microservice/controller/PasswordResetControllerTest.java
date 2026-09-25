package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.config.SecurityConfig;
import dev.lucas.user_microservice.config.TokenConfig;
import dev.lucas.user_microservice.security.RateLimiter;
import dev.lucas.user_microservice.security.TokenRevocationService;
import dev.lucas.user_microservice.service.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordResetController.class)
@Import({SecurityConfig.class, PasswordResetControllerTest.TestTokens.class})
class PasswordResetControllerTest {

    @TestConfiguration
    static class TestTokens {
        @Bean
        TokenConfig tokenConfig() {
            return new TokenConfig("segredo-de-teste");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private RateLimiter rateLimiter;

    @Test
    @DisplayName("Esqueci a senha é público e sempre responde 202, normalizando o e-mail")
    void forgotIsPublicAndGeneric() throws Exception {
        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"  ANA@X.COM \"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").exists());

        verify(passwordResetService).requestReset("ana@x.com");
    }

    @Test
    @DisplayName("E-mail inválido retorna 400")
    void forgotRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest());

        verify(passwordResetService, never()).requestReset(anyString());
    }

    @Test
    @DisplayName("Redefinição com link válido retorna 204")
    void resetReturnsNoContent() throws Exception {
        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\",\"newPassword\":\"novaSenha123\"}"))
                .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword("abc", "novaSenha123");
    }

    @Test
    @DisplayName("Redefinição com link expirado retorna 400 com a mensagem")
    void resetWithExpiredLink() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Link inválido ou expirado."))
                .when(passwordResetService).resetPassword(eq("velho"), any());

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"velho\",\"newPassword\":\"novaSenha123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Pedidos em excesso de redefinição retornam 429")
    void forgotIsRateLimited() throws Exception {
        when(rateLimiter.retryAfterSeconds(eq("forgot:127.0.0.1"), anyInt(), any())).thenReturn(600L);

        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@x.com\"}"))
                .andExpect(status().isTooManyRequests());

        verify(passwordResetService, never()).requestReset(anyString());
    }
}
