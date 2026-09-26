package dev.lucas.user_microservice.controller;

import dev.lucas.user_microservice.client.CarCacheClient;
import dev.lucas.user_microservice.config.JWTUserData;
import dev.lucas.user_microservice.dtos.UserResponse;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class CnhDocumentController {

    private final UserService userService;
    private final CarCacheClient carCacheClient;

    @PutMapping(value = "/users/me/cnh-document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse upload(@AuthenticationPrincipal JWTUserData principal,
                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                               @RequestParam("file") MultipartFile file) {
        UserModel user = userService.updateCnhDocument(principal.id(), file);
        carCacheClient.publish(user, authorization.substring("Bearer ".length()));
        return UserResponse.from(user);
    }

    @GetMapping("/users/me/cnh-document")
    public ResponseEntity<Resource> mine(@AuthenticationPrincipal JWTUserData principal) {
        return pdf(principal.id());
    }

    @GetMapping("/users/{id}/cnh-document")
    public ResponseEntity<Resource> ofUser(@PathVariable Long id, @AuthenticationPrincipal JWTUserData principal) {
        if (!principal.isAdmin() && !principal.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você só pode acessar a sua própria CNH.");
        }
        return pdf(id);
    }

    private ResponseEntity<Resource> pdf(Long userId) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("cnh-" + userId + ".pdf").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(userService.cnhDocument(userId));
    }
}
