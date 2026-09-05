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

        try {
            String result = rentalService.rentCar(99L, 42L);
            assertThat(result).isNull();
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).contains("não encontrado");
        }
    }

    @Test
    @DisplayName("Deve devolver carro alugado com sucesso")
    void shouldReturnRentedCar() {
        availableCar.setStatus(CarStatus.RENTED);
        availableCar.setRentalDate(LocalDate.now());
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.returnCar(1L);

        assertThat(result).contains("devolvido");
        verify(carRepository).save(any(CarModel.class));
    }

    @Test
    @DisplayName("Não deve devolver carro já disponível")
    void shouldNotReturnAlreadyAvailableCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.returnCar(1L);

        assertThat(result).contains("disponível");
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
}