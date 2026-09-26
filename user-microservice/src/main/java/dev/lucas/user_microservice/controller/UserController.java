package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.config.JWTUserData;
import dev.lucas.user_microservice.dtos.AdminUserRequest;
import dev.lucas.user_microservice.dtos.ProfileUpdateRequest;
import dev.lucas.user_microservice.dtos.RoleUpdateRequest;
import dev.lucas.user_microservice.dtos.UserRequest;
import dev.lucas.user_microservice.dtos.UserResponse;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.security.TokenRevocationService;
import dev.lucas.user_microservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final TokenRevocationService tokenRevocationService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
        UserModel userModel = toModel(request);
        UserModel savedUser = userService.saveUser(userModel);
        return ResponseEntity.status(201).body(UserResponse.from(savedUser));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUserAsAdmin(@Valid @RequestBody AdminUserRequest request) {
        UserModel user = new UserModel();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setCpf(request.cpf());
        user.setCnh(request.cnh());
        user.setPassword(request.password());
        user.setRole(request.role());
        return ResponseEntity.status(201).body(UserResponse.from(userService.saveUser(user)));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id,
                                                    @AuthenticationPrincipal JWTUserData principal) {
        requireSelfOrAdmin(principal, id);
        Optional<UserModel> userOpt = userService.getUserById(id);
        return userOpt.map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody ProfileUpdateRequest request,
                                                   @AuthenticationPrincipal JWTUserData principal) {
        requireSelfOrAdmin(principal, id);
        Optional<UserModel> updatedOpt = userService.updateProfile(id, request);
        return updatedOpt.map(user -> ResponseEntity.ok(UserResponse.from(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable Long id,
                                                   @Valid @RequestBody RoleUpdateRequest request,
                                                   @AuthenticationPrincipal JWTUserData principal) {
        if (principal.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Um administrador não pode alterar o próprio papel.");
        }
        return userService.changeRole(id, request.role())
                .map(user -> {
                    tokenRevocationService.revokeAll(user.getId());
                    return ResponseEntity.ok(UserResponse.from(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id,
                                           @AuthenticationPrincipal JWTUserData principal) {
        requireSelfOrAdmin(principal, id);
        Optional<UserModel> userOpt = userService.getUserById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        tokenRevocationService.revokeAll(id);
        return ResponseEntity.noContent().build();
    }

    private void requireSelfOrAdmin(JWTUserData principal, Long targetId) {
        if (!principal.isAdmin() && !principal.id().equals(targetId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você só pode acessar a sua própria conta.");
        }
    }

    private UserModel toModel(UserRequest request) {
        UserModel user = new UserModel();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setCpf(request.cpf());
        user.setCnh(request.cnh());
        user.setPassword(request.password());
        return user;
    }
}
