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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, UserControllerSecurityTest.TestTokens.class})
class UserControllerSecurityTest {

    private static final String SECRET = "segredo-de-teste";

    @TestConfiguration
    static class TestTokens {
        @Bean
        TokenConfig tokenConfig() {
            return new TokenConfig(SECRET);
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

    private UserModel usuario(Long id, UserRole role) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setName("Usuário " + id);
        user.setEmail("user" + id + "@email.com");
        user.setRole(role);
        return user;
    }

    private String bearer(Long id, UserRole role) {
        return "Bearer " + tokenConfig.generateToken(usuario(id, role), 0);
    }

    @Test
    @DisplayName("Cadastro é público")
    void registerIsPublic() throws Exception {
        when(userService.saveUser(any())).thenAnswer(i -> {
            UserModel u = i.getArgument(0);
            u.setId(10L);
            return u;
        });

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@x.com\",\"password\":\"senhaForte123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("Cadastro ignora role enviado no corpo e sempre cria USER")
    void registerCannotEscalateRole() throws Exception {
        when(userService.saveUser(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@x.com\",\"password\":\"senhaForte123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userService).saveUser(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    @DisplayName("Admin cria outro administrador pela plataforma")
    void adminCanCreateAdmin() throws Exception {
        when(userService.saveUser(any())).thenAnswer(i -> {
            UserModel u = i.getArgument(0);
            u.setId(11L);
            return u;
        });

        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(99L, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bia\",\"email\":\" BIA@X.COM \",\"password\":\"senhaForte123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.email").value("bia@x.com"));
    }

    @Test
    @DisplayName("Usuário comum não pode criar contas pela área administrativa")
    void userCannotCreateUsersAsAdmin() throws Exception {
        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(1L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bia\",\"email\":\"bia@x.com\",\"password\":\"senhaForte123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verify(userService, never()).saveUser(any());
    }

    @Test
    @DisplayName("Criar usuário pela área administrativa sem token retorna 401")
    void createUserAsAdminRequiresToken() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bia\",\"email\":\"bia@x.com\",\"password\":\"senhaForte123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Criar usuário pela área administrativa exige o papel")
    void createUserAsAdminRequiresRole() throws Exception {
        mockMvc.perform(post("/users")
                        .header("Authorization", bearer(99L, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bia\",\"email\":\"bia@x.com\",\"password\":\"senhaForte123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    @Test
    @DisplayName("Listar usuários sem token retorna 401")
    void listUsersWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Listar usuários como USER retorna 403")
    void listUsersAsUserIsForbidden() throws Exception {
        mockMvc.perform(get("/users").header("Authorization", bearer(1L, UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Listar usuários como ADMIN retorna 200")
    void listUsersAsAdminIsAllowed() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(usuario(1L, UserRole.USER)));

        mockMvc.perform(get("/users").header("Authorization", bearer(99L, UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("USER consegue ver a própria conta")
    void userCanReadOwnAccount() throws Exception {
        when(userService.getUserById(1L)).thenReturn(Optional.of(usuario(1L, UserRole.USER)));

        mockMvc.perform(get("/users/1").header("Authorization", bearer(1L, UserRole.USER)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER não consegue ver a conta de outro usuário")
    void userCannotReadOtherAccount() throws Exception {
        mockMvc.perform(get("/users/2").header("Authorization", bearer(1L, UserRole.USER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).getUserById(anyLong());
    }

    @Test
    @DisplayName("ADMIN consegue ver qualquer conta")
    void adminCanReadAnyAccount() throws Exception {
        when(userService.getUserById(2L)).thenReturn(Optional.of(usuario(2L, UserRole.USER)));

        mockMvc.perform(get("/users/2").header("Authorization", bearer(99L, UserRole.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER não consegue apagar a conta de outro usuário")
    void userCannotDeleteOtherAccount() throws Exception {
        mockMvc.perform(delete("/users/2").header("Authorization", bearer(1L, UserRole.USER)))
                .andExpect(status().isForbidden());

        verify(userService, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("USER não consegue promover ninguém")
    void userCannotChangeRoles() throws Exception {
        mockMvc.perform(patch("/users/1/role")
                        .header("Authorization", bearer(1L, UserRole.USER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verify(userService, never()).changeRole(anyLong(), any());
    }

    @Test
    @DisplayName("ADMIN promove outro usuário")
    void adminCanPromoteUser() throws Exception {
        when(userService.changeRole(5L, UserRole.ADMIN)).thenReturn(Optional.of(usuario(5L, UserRole.ADMIN)));

        mockMvc.perform(patch("/users/5/role")
                        .header("Authorization", bearer(99L, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        verify(tokenRevocationService).revokeAll(5L);
    }

    @Test
    @DisplayName("ADMIN não pode alterar o próprio papel")
    void adminCannotChangeOwnRole() throws Exception {
        mockMvc.perform(patch("/users/99/role")
                        .header("Authorization", bearer(99L, UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        verify(userService, never()).changeRole(anyLong(), any());
    }

    @Test
    @DisplayName("Token inválido é tratado como anônimo")
    void invalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/users/1").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Preflight CORS do front-end é liberado")
    void corsPreflightFromFrontendIsAllowed() throws Exception {
        mockMvc.perform(options("/users")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("Token revogado é tratado como anônimo")
    void revokedTokenIsUnauthorized() throws Exception {
        when(tokenRevocationService.isRevoked(any(), anyLong(), anyLong())).thenReturn(true);

        mockMvc.perform(get("/users").header("Authorization", bearer(99L, UserRole.ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Remover a conta derruba todos os tokens do usuário")
    void deleteRevokesAllTokens() throws Exception {
        when(userService.getUserById(1L)).thenReturn(Optional.of(usuario(1L, UserRole.USER)));

        mockMvc.perform(delete("/users/1").header("Authorization", bearer(1L, UserRole.USER)))
                .andExpect(status().isNoContent());

        verify(tokenRevocationService).revokeAll(1L);
    }

    @Test
    @DisplayName("Cadastro inválido retorna 400 com a mensagem do campo")
    void invalidRegisterReturnsFieldMessage() throws Exception {
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"nao-e-email\",\"password\":\"senhaForte123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("E-mail inválido."))
                .andExpect(jsonPath("$.errors.email").exists());

        verify(userService, never()).saveUser(any());
    }

    @Test
    @DisplayName("Senha curta é recusada no cadastro")
    void shortPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@x.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    @DisplayName("Cadastro sanitiza nome, e-mail e documentos antes de salvar")
    void registerSanitizesInput() throws Exception {
        when(userService.saveUser(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  <script>alert(1)</script>Ana   Souza \","
                                + "\"email\":\"  ANA@X.COM \",\"cpf\":\"123.456.789-00\","
                                + "\"cnh\":\"987 654 321 00\",\"password\":\"senhaForte123\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<UserModel> captor = ArgumentCaptor.forClass(UserModel.class);
        verify(userService).saveUser(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("alert(1)Ana Souza");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@x.com");
        assertThat(captor.getValue().getCpf()).isEqualTo("12345678900");
        assertThat(captor.getValue().getCnh()).isEqualTo("98765432100");
    }

    @Test
    @DisplayName("Cadastro acima do limite retorna 429")
    void registerIsRateLimited() throws Exception {
        when(rateLimiter.retryAfterSeconds(any(), org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(120L);

        mockMvc.perform(post("/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@x.com\",\"password\":\"senhaForte123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "120"));

        verify(userService, never()).saveUser(any());
    }
}
