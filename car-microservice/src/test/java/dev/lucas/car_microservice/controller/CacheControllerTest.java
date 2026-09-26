package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.config.SecurityConfig;
import dev.lucas.car_microservice.dto.UserCacheDto;
import dev.lucas.car_microservice.security.TestJwt;
import dev.lucas.car_microservice.security.TokenRevocationChecker;
import dev.lucas.car_microservice.service.CacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CacheController.class)
@Import({SecurityConfig.class, TestJwt.Config.class})
class CacheControllerTest {

    private static final String USER_5_JSON =
            "{\"id\":\"5\",\"name\":\"Ana\",\"cpf\":\"123\",\"email\":\"ana@x.com\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenRevocationChecker revocationChecker;

    @MockitoBean
    private CacheService cacheService;

    @Test
    @DisplayName("Gravar cache sem token retorna 401")
    void saveWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/cache/user").contentType(MediaType.APPLICATION_JSON).content(USER_5_JSON))
                .andExpect(status().isUnauthorized());

        verify(cacheService, never()).saveUser(any(), any());
    }

    @Test
    @DisplayName("Usuário grava o próprio cache, como faz o login")
    void userCanCacheSelf() throws Exception {
        mockMvc.perform(post("/cache/user")
                        .header("Authorization", TestJwt.user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_5_JSON))
                .andExpect(status().isOk());

        verify(cacheService).saveUser(any(), any());
    }

    @Test
    @DisplayName("Usuário não grava cache com dados de outra pessoa")
    void userCannotCacheSomeoneElse() throws Exception {
        mockMvc.perform(post("/cache/user")
                        .header("Authorization", TestJwt.user(6L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(USER_5_JSON))
                .andExpect(status().isForbidden());

        verify(cacheService, never()).saveUser(any(), any());
    }

    @Test
    @DisplayName("Usuário lê o próprio cache e não o de outro")
    void userReadsOnlyOwnCache() throws Exception {
        when(cacheService.getUser("5")).thenReturn(new UserCacheDto("5", "Ana", "123", "ana@x.com", true));

        mockMvc.perform(get("/cache/user/5").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana"));
        mockMvc.perform(get("/cache/user/5").header("Authorization", TestJwt.user(6L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN lê o cache de qualquer usuário")
    void adminReadsAnyCache() throws Exception {
        when(cacheService.getUser("5")).thenReturn(new UserCacheDto("5", "Ana", "123", "ana@x.com", true));

        mockMvc.perform(get("/cache/user/5").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk());
    }
}
