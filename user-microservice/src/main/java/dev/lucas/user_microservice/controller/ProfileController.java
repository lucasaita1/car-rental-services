package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.config.JWTUserData;
import dev.lucas.user_microservice.dtos.PasswordChangeRequest;
import dev.lucas.user_microservice.dtos.ProfileUpdateRequest;
import dev.lucas.user_microservice.dtos.UserResponse;
import dev.lucas.user_microservice.security.TokenRevocationService;
import dev.lucas.user_microservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final TokenRevocationService tokenRevocationService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal JWTUserData principal) {
        return userService.getUserById(principal.id())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal JWTUserData principal,
                               @Valid @RequestBody ProfileUpdateRequest request) {
        return userService.updateProfile(principal.id(), request)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal JWTUserData principal,
                                               @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(principal.id(), request.currentPassword(), request.newPassword());
        tokenRevocationService.revokeAll(principal.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadPhoto(@AuthenticationPrincipal JWTUserData principal,
                                    @RequestParam("file") MultipartFile file) {
        return UserResponse.from(userService.updatePhoto(principal.id(), file));
    }

    @DeleteMapping("/photo")
    public UserResponse removePhoto(@AuthenticationPrincipal JWTUserData principal) {
        return UserResponse.from(userService.removePhoto(principal.id()));
    }
}
