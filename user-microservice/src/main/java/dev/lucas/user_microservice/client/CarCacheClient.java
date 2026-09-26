package dev.lucas.user_microservice.client;

import dev.lucas.user_microservice.dtos.UserCacheDto;
import dev.lucas.user_microservice.entity.UserModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CarCacheClient {

    private final RestTemplate restTemplate;

    @Value("${CAR_SERVICE_URL:http://localhost:8082}")
    private String carServiceUrl;

    public static UserCacheDto toCacheDto(UserModel user) {
        return new UserCacheDto(
                user.getId().toString(),
                user.getName(),
                user.getCpf(),
                user.getEmail(),
                user.hasCnhDocument());
    }

    public void publish(UserModel user, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.postForObject(carServiceUrl + "/cache/user",
                    new HttpEntity<>(toCacheDto(user), headers), Void.class);
        } catch (Exception e) {
            log.warn("Falha ao enviar dados do usuário {} para o car-service: {}", user.getId(), e.getMessage());
        }
    }
}
