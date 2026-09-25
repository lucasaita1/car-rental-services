package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.config.SecurityConfig;
import dev.lucas.user_microservice.config.TokenConfig;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.security.RateLimiter;
import dev.lucas.user_microservice.security.TokenRevocationService;
import dev.lucas.user_microservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, ProfileControllerTest.TestTokens.class})
class ProfileControllerTest {

    @TestConfiguration
    static class TestTokens {
        @Bean
        TokenConfig tokenConfig() {
            return new TokenConfig("segredo-de-teste");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenConfig tokenConfig;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private RateLimiter rateLimiter;

    private UserModel ana() {
        UserModel user = new UserModel();
        user.setId(5L);
        user.setName("Ana");
        user.setEmail("ana@x.com");
        user.setRole(UserRole.USER);
        return user;
    }

    private String bearer() {
        return "Bearer " + tokenConfig.generateToken(ana(), 0);
    }

    @Test
    @DisplayName("Perfil exige login")
    void profileRequiresLogin() throws Exception {
        mockMvc.perform(get("/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /users/me devolve o usuário do token, e não um id do path")
    void returnsOwnProfile() throws Exception {
        when(userService.getUserById(5L)).thenReturn(Optional.of(ana()));

        mockMvc.perform(get("/users/me").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.email").value("ana@x.com"));
    }

    @Test
    @DisplayName("Atualiza os dados do próprio perfil")
    void updatesOwnProfile() throws Exception {
        UserModel atualizado = ana();
        atualizado.setName("Ana Souza");
        when(userService.updateProfile(eq(5L), any())).thenReturn(Optional.of(atualizado));

        mockMvc.perform(put("/users/me")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Souza\",\"email\":\"ana@x.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"));
    }

    @Test
    @DisplayName("Perfil com e-mail inválido retorna 400")
    void rejectsInvalidProfile() throws Exception {
        mockMvc.perform(put("/users/me")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"invalido\"}"))
                .andExpect(status().isBadRequest());

        verify(userService, never()).updateProfile(anyLong(), any());
    }

    @Test
    @DisplayName("Trocar a senha derruba todas as sessões do usuário")
    void changePasswordRevokesSessions() throws Exception {
        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"antiga123\",\"newPassword\":\"novaSenha123\"}"))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(5L, "antiga123", "novaSenha123");
        verify(tokenRevocationService).revokeAll(5L);
    }

    @Test
    @DisplayName("Nova senha curta é recusada")
    void rejectsShortNewPassword() throws Exception {
        mockMvc.perform(put("/users/me/password")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"antiga123\",\"newPassword\":\"123\"}"))
                .andExpect(status().isBadRequest());

        verify(tokenRevocationService, never()).revokeAll(anyLong());
    }

    @Test
    @DisplayName("Envia a foto de perfil e recebe a URL pública")
    void uploadsProfilePhoto() throws Exception {
        UserModel comFoto = ana();
        comFoto.setPhotoPath("users/abc.png");
        when(userService.updatePhoto(eq(5L), any())).thenReturn(comFoto);

        mockMvc.perform(multipart("/users/me/photo")
                        .file(new MockMultipartFile("file", "eu.png", "image/png", new byte[]{1}))
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("/files/users/abc.png"));
    }

    @Test
    @DisplayName("Remove a foto de perfil")
    void removesProfilePhoto() throws Exception {
        when(userService.removePhoto(5L)).thenReturn(ana());

        mockMvc.perform(delete("/users/me/photo").header("Authorization", bearer()))
                .andExpect(status().isOk());
    }
}
