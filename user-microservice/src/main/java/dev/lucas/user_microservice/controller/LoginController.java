package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.config.JWTUserData;
import dev.lucas.user_microservice.config.TokenConfig;
import dev.lucas.user_microservice.dtos.LoginRequest;
import dev.lucas.user_microservice.dtos.UserCacheDto;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.security.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final TokenRevocationService tokenRevocationService;

    @Value("${CAR_SERVICE_URL:http://localhost:8082}")
    private String carServiceUrl;

    /**
     * Faz login, gera o JWT e envia os dados do usuário autenticado
     * para o CarService salvar em cache Redis.
     */
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            // Cria token de autenticação com email e senha
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.email(),
                            loginRequest.password()
                    )
            );

            // Pega o usuário autenticado
            UserModel user = (UserModel) authentication.getPrincipal();

            // Gera o JWT
            String token = tokenConfig.generateToken(user, tokenRevocationService.currentVersion(user.getId()));

            // Monta DTO para enviar pro CarService
            UserCacheDto cacheDto = new UserCacheDto(
                    user.getId().toString(),
                    user.getName(),
                    user.getCpf(),
                    user.getEmail()
            );

            // Envia dados pro CarService
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            try {
                restTemplate.postForObject(carServiceUrl + "/cache/user",
                        new HttpEntity<>(cacheDto, headers), Void.class);
            } catch (Exception e) {
                System.out.println("Falha ao enviar dados para o CarService: " + e.getMessage());
            }

            // Retorna token e dados do usuário
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", cacheDto,
                    "role", user.getRole()
            ));

        } catch (BadCredentialsException exception) {
            throw new BadCredentialsException("Email ou senha inválidos!");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal JWTUserData principal) {
        tokenRevocationService.revoke(principal.jti(), principal.expiresAt());
        return ResponseEntity.noContent().build();
    }
}

