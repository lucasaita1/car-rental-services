package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.client.CarCacheClient;
import dev.lucas.user_microservice.config.JWTUserData;
import dev.lucas.user_microservice.config.TokenConfig;
import dev.lucas.user_microservice.dtos.LoginRequest;
import dev.lucas.user_microservice.dtos.UserCacheDto;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.security.TokenRevocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;
    private final UserRepository userRepository;
    private final CarCacheClient carCacheClient;
    private final TokenRevocationService tokenRevocationService;

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

            UserCacheDto cacheDto = CarCacheClient.toCacheDto(user);
            carCacheClient.publish(user, token);

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

