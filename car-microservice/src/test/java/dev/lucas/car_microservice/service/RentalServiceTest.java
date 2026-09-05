package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RentalService rentalService;

    private CarModel availableCar;

    @BeforeEach
    void setUp() {
        availableCar = new CarModel(
                1L,
                "Civic",
                "Preto",
                "ABC-1D23",
                2024,
                null,
                null,
                CarStatus.AVAILABLE,
                null
        );
    }

    @Test
    @DisplayName("Deve alugar carro disponível com sucesso")
    void shouldRentAvailableCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", "Lucas");
        userData.put("email", "lucas@email.com");
        userData.put("cpf", "12345678900");

        when(valueOperations.get("user:42")).thenReturn(userData);

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("sucesso");

        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository, times(1)).save(captor.capture());

        CarModel saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(CarStatus.RENTED);
        assertThat(saved.getRentalDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Não deve alugar carro que já está alugado")
    void shouldNotRentAlreadyRentedCar() {
        availableCar.setStatus(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("já está alugado");
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve alugar quando usuário não está no cache")
    void shouldNotRentWhenUserNotInCache() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42")).thenReturn(null);

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("não encontrado no cache");
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando o carro não existe no aluguel")
    void shouldThrowWhenCarNotFoundOnRent() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        // Fixa o contrato: alugar carro inexistente estoura, e não devolve mensagem.
        // Diferente do returnCar, que devolve texto. A assimetria é intencional no serviço.
        assertThatThrownBy(() -> rentalService.rentCar(99L, 42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Carro não encontrado.");

        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve devolver carro alugado com sucesso")
    void shouldReturnRentedCar() {
        availableCar.setStatus(CarStatus.RENTED);
        availableCar.setRentalDate(LocalDate.now());
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.returnCar(1L);

        assertThat(result).contains("devolvido");

        // Verificar apenas que save() foi chamado não prova que o carro voltou
        // ao estoque. O que importa é o estado persistido.
        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository).save(captor.capture());

        CarModel devolvido = captor.getValue();
        assertThat(devolvido.getStatus()).isEqualTo(CarStatus.AVAILABLE);
        assertThat(devolvido.getRentalDate()).isNull();
    }

    @Test
    @DisplayName("Não deve devolver carro já disponível")
    void shouldNotReturnAlreadyAvailableCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.returnCar(1L);

        // A palavra "disponível" aparece nas duas mensagens do fluxo de devolução,
        // então a asserção precisa ser sobre o trecho que só existe na recusa.
        assertThat(result).contains("não há aluguel ativo");
        assertThat(availableCar.getStatus()).isEqualTo(CarStatus.AVAILABLE);
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar mensagem quando carro não existe na devolução")
    void shouldReturnMessageWhenCarNotFoundOnReturn() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        String result = rentalService.returnCar(99L);

        assertThat(result).contains("não encontrado");
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve consultar o cache na chave user:{userId} acordada com o user-microservice")
    void shouldReadCacheUnderAgreedKey() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:7")).thenReturn(Map.of("name", "Ana", "email", "ana@x.com", "cpf", "1"));

        rentalService.rentCar(1L, 7L);

        // Se o formato da chave divergir do que o CacheService grava no login,
        // todo aluguel passa a falhar por "usuário não encontrado no cache".
        verify(valueOperations).get("user:7");
    }

    @Test
    @DisplayName("Não deve alugar carro que está em manutenção")
    void shouldNotRentCarUnderMaintenance() {
        availableCar.setStatus(CarStatus.MAINTENANCE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("em manutenção");
        // A recusa acontece antes de consultar o cache, então nada é persistido.
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar quem alugou o veículo")
    void shouldPersistRenterId() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42")).thenReturn(Map.of("name", "Lucas", "email", "l@x.com", "cpf", "1"));

        rentalService.rentCar(1L, 42L);

        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository).save(captor.capture());

        assertThat(captor.getValue().getUserId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Devolução deve liberar o vínculo com o locatário")
    void shouldClearRenterIdOnReturn() {
        availableCar.setStatus(CarStatus.RENTED);
        availableCar.setUserId(42L);
        availableCar.setRentalDate(LocalDate.now());
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        rentalService.returnCar(1L);

        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository).save(captor.capture());

        // Um carro disponível não pode continuar apontando para o locatário anterior.
        assertThat(captor.getValue().getUserId()).isNull();
    }

    @Test
    @DisplayName("Aluguel não deve alterar a data de devolução prevista")
    void shouldNotTouchReturnDateOnRent() {
        availableCar.setReturnDate(LocalDate.of(2026, 12, 25));
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42")).thenReturn(Map.of("name", "Lucas", "email", "l@x.com", "cpf", "1"));

        rentalService.rentCar(1L, 42L);

        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository).save(captor.capture());

        assertThat(captor.getValue().getReturnDate()).isEqualTo(LocalDate.of(2026, 12, 25));
    }
}