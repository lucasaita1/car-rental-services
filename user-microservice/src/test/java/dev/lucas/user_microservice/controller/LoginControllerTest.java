package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.client.CarCacheClient;
import dev.lucas.user_microservice.config.SecurityConfig;
import dev.lucas.user_microservice.config.TokenConfig;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.security.RateLimiter;
import dev.lucas.user_microservice.security.TokenRevocationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@Import({SecurityConfig.class, CarCacheClient.class, LoginControllerTest.TestTokens.class})
class LoginControllerTest {

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
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private TokenRevocationService tokenRevocationService;

    @MockitoBean
    private RateLimiter rateLimiter;

    private UserModel admin() {
        UserModel user = new UserModel();
        user.setId(1L);
        user.setName("Admin");
        user.setEmail("admin@x.com");
        user.setRole(UserRole.ADMIN);
        return user;
    }

    @Test
    @DisplayName("Login devolve token com o papel e a versão atual do usuário")
    void loginReturnsTokenWithRoleAndVersion() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(admin(), null));
        when(tokenRevocationService.currentVersion(1L)).thenReturn(4L);

        String body = mockMvc.perform(post("/auth/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@x.com\",\"password\":\"senha\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();

        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");
        assertThat(tokenConfig.verifyToken(token).orElseThrow().version()).isEqualTo(4L);
    }

    @Test
    @DisplayName("Login avisa o car-service levando o token como Bearer")
    void loginForwardsBearerToCarService() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(admin(), null));

        mockMvc.perform(post("/auth/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@x.com\",\"password\":\"senha\"}"))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Object>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForObject(eq("http://localhost:8082/cache/user"), captor.capture(), eq(Void.class));
        assertThat(captor.getValue().getHeaders().getFirst("Authorization")).startsWith("Bearer ");
    }

    @Test
    @DisplayName("Senha errada retorna 401")
    void wrongPasswordIsUnauthorized() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        mockMvc.perform(post("/auth/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@x.com\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout revoga o jti do token usado")
    void logoutRevokesCurrentToken() throws Exception {
        String token = tokenConfig.generateToken(admin(), 0);
        String jti = tokenConfig.verifyToken(token).orElseThrow().jti();

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        verify(tokenRevocationService).revoke(eq(jti), any());
    }

    @Test
    @DisplayName("Logout sem token retorna 401")
    void logoutWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login acima do limite retorna 429 sem tentar autenticar")
    void loginIsRateLimited() throws Exception {
        when(rateLimiter.retryAfterSeconds(eq("login:127.0.0.1"), eq(5), any())).thenReturn(42L);

        mockMvc.perform(post("/auth/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@x.com\",\"password\":\"senha\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Muitas tentativas. Tente novamente em 42 segundos."));

        verify(authenticationManager, org.mockito.Mockito.never()).authenticate(any());
    }
}
