package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    private CarModel car;

    @BeforeEach
    void setUp() {
        car = new CarModel(
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
    @DisplayName("Deve salvar um carro com sucesso")
    void shouldSaveCar() {
        when(carRepository.save(any(CarModel.class))).thenReturn(car);

        CarModel saved = carService.save(car);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getModel()).isEqualTo("Civic");
        verify(carRepository, times(1)).save(car);
    }

    @Test
    @DisplayName("Deve buscar carro por id existente")
    void shouldFindById() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        CarModel found = carService.findById(1L);

        assertThat(found).isNotNull();
        assertThat(found.getPlate()).isEqualTo("ABC-1D23");
        verify(carRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção quando carro não for encontrado por id")
    void shouldThrowWhenCarNotFound() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.findById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Car not found");
    }

    @Test
    @DisplayName("Deve listar todos os carros")
    void shouldFindAll() {
        CarModel outro = new CarModel(
                2L, "Onix", "Branco", "XYZ-9Z99", 2022,
                LocalDate.now(), null, CarStatus.RENTED, null
        );
        when(carRepository.findAll()).thenReturn(List.of(car, outro));

        List<CarModel> cars = carService.findAll();

        assertThat(cars).hasSize(2);
        verify(carRepository).findAll();
    }

    @Test
    @DisplayName("Deve deletar carro por id")
    void shouldDeleteById() {
        carService.deleteById(1L);

        verify(carRepository, times(1)).deleteById(1L);
    }
}