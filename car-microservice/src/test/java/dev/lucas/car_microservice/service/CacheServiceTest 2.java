package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.dto.UserCacheDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * O CacheService é o contrato entre dois microsserviços: o user-microservice
 * escreve a sessão aqui no login e o RentalService lê na hora do aluguel.
 * O formato da chave é acordo entre eles, e um desalinhamento faz todo aluguel
 * falhar com "usuário não encontrado no cache" sem erro visível em log.
 */
@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    private UserCacheDto usuario() {
        return new UserCacheDto("42", "Lucas", "12345678900", "lucas@email.com");
    }

    @Test
    @DisplayName("Deve gravar o usuário na chave user:{id} com o TTL informado")
    void shouldSaveUnderNamespacedKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserCacheDto user = usuario();

        cacheService.saveUser(user, Duration.ofMinutes(120));

        // A chave precisa bater exatamente com a que o RentalService monta ao alugar.
        verify(valueOperations).set(eq("user:42"), eq(user), eq(Duration.ofMinutes(120)));
    }

    @Test
    @DisplayName("Deve respeitar o TTL recebido, sem sobrescrever por um valor fixo")
    void shouldHonourProvidedTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.saveUser(usuario(), Duration.ofSeconds(30));

        verify(valueOperations).set(eq("user:42"), eq(usuario()), eq(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("Deve recuperar o usuário gravado a partir do id")
    void shouldReadUserBack() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        UserCacheDto user = usuario();
        when(valueOperations.get("user:42")).thenReturn(user);

        UserCacheDto encontrado = cacheService.getUser("42");

        assertThat(encontrado).isNotNull();
        assertThat(encontrado.getName()).isEqualTo("Lucas");
        assertThat(encontrado.getEmail()).isEqualTo("lucas@email.com");
        assertThat(encontrado.getCpf()).isEqualTo("12345678900");
    }

    @Test
    @DisplayName("Deve devolver nulo quando a chave expirou ou nunca existiu")
    void shouldReturnNullWhenAbsent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:404")).thenReturn(null);

        assertThat(cacheService.getUser("404")).isNull();
    }

    @Test
    @DisplayName("Deve devolver nulo quando o valor em cache não é um UserCacheDto")
    void shouldReturnNullWhenCachedValueHasUnexpectedType() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Map<String, Object> comoMapa = new HashMap<>();
        comoMapa.put("name", "Lucas");
        when(valueOperations.get("user:42")).thenReturn(comoMapa);

        // Se o serializer do Redis mudar e o valor voltar como Map em vez de DTO,
        // o serviço devolve nulo em vez de estourar ClassCastException no controller.
        assertThat(cacheService.getUser("42")).isNull();
    }

    @Test
    @DisplayName("Chaves de usuários distintos não devem colidir")
    void shouldNamespaceKeysPerUser() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cacheService.saveUser(new UserCacheDto("1", "Ana", "1", "ana@x.com"), Duration.ofMinutes(1));
        cacheService.saveUser(new UserCacheDto("2", "Bia", "2", "bia@x.com"), Duration.ofMinutes(1));

        verify(valueOperations).set(eq("user:1"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Duration.class));
        verify(valueOperations).set(eq("user:2"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(Duration.class));
    }
}
