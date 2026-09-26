package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.client.CarCacheClient;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CnhDocumentController.class)
@Import({SecurityConfig.class, CnhDocumentControllerTest.TestTokens.class})
class CnhDocumentControllerTest {

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
    private CarCacheClient carCacheClient;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private RateLimiter rateLimiter;

    private UserModel usuario(Long id, UserRole role) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setEmail("user" + id + "@x.com");
        user.setRole(role);
        return user;
    }

    private String bearer(Long id, UserRole role) {
        return "Bearer " + tokenConfig.generateToken(usuario(id, role), 0);
    }

    private static MockMultipartFile pdf() {
        return new MockMultipartFile("file", "cnh.pdf", "application/pdf", "%PDF-1.7".getBytes());
    }

    @Test
    @DisplayName("Enviar a CNH atualiza o cache do car-service com o mesmo token")
    void uploadPublishesCache() throws Exception {
        UserModel salvo = usuario(5L, UserRole.USER);
        salvo.setCnhDocumentPath("documents/cnh/x.pdf");
        salvo.setCnhDocumentUploadedAt(Instant.now());
        when(userService.updateCnhDocument(eq(5L), any())).thenReturn(salvo);

        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/cnh-document").file(pdf())
                        .header("Authorization", bearer(5L, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnhDocument").value(true))
                .andExpect(jsonPath("$.cnhDocumentUploadedAt").exists());

        verify(carCacheClient).publish(eq(salvo), startsWith("ey"));
    }

    @Test
    @DisplayName("Enviar a CNH sem token retorna 401")
    void uploadRequiresToken() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/cnh-document").file(pdf()))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateCnhDocument(anyLong(), any());
    }

    @Test
    @DisplayName("Dono baixa a própria CNH como PDF inline, sem cache")
    void ownerDownloadsPdf() throws Exception {
        when(userService.cnhDocument(5L)).thenReturn(new ByteArrayResource("%PDF-1.7".getBytes()));

        mockMvc.perform(get("/users/me/cnh-document").header("Authorization", bearer(5L, UserRole.USER)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Cache-Control", "private, no-store"));
    }

    @Test
    @DisplayName("USER não baixa a CNH de outra pessoa")
    void userCannotDownloadOthersCnh() throws Exception {
        mockMvc.perform(get("/users/6/cnh-document").header("Authorization", bearer(5L, UserRole.USER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).cnhDocument(anyLong());
    }

    @Test
    @DisplayName("ADMIN baixa a CNH de qualquer cliente")
    void adminDownloadsAnyCnh() throws Exception {
        when(userService.cnhDocument(6L)).thenReturn(new ByteArrayResource("%PDF-1.7".getBytes()));

        mockMvc.perform(get("/users/6/cnh-document").header("Authorization", bearer(99L, UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
