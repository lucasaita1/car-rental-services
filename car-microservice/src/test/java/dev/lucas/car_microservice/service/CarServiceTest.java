package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import dev.lucas.car_microservice.repository.RentalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private RentalRepository rentalRepository;

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

    @Test
    @DisplayName("Não deve remover carro com locação ativa")
    void shouldNotDeleteRentedCar() {
        when(rentalRepository.existsByCarIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> carService.deleteById(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("locação ativa");
        verify(carRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Deve atualizar os dados de catálogo e o status do carro")
    void shouldUpdateCar() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(any(CarModel.class))).thenAnswer(i -> i.getArgument(0));

        CarModel atualizado = carService.update(1L, CarRequestDto.builder()
                .model("Civic Touring").color("Branco").plate("XYZ-0A00").year(2025)
                .status(CarStatus.MAINTENANCE).build());

        assertThat(atualizado.getModel()).isEqualTo("Civic Touring");
        assertThat(atualizado.getColor()).isEqualTo("Branco");
        assertThat(atualizado.getPlate()).isEqualTo("XYZ-0A00");
        assertThat(atualizado.getYear()).isEqualTo(2025);
        assertThat(atualizado.getStatus()).isEqualTo(CarStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("Edição sem status deve manter o status atual")
    void shouldKeepStatusWhenNotProvided() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(carRepository.save(any(CarModel.class))).thenAnswer(i -> i.getArgument(0));

        CarModel atualizado = carService.update(1L, CarRequestDto.builder().model("Civic").build());

        assertThat(atualizado.getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Não deve permitir marcar carro como RENTED pela edição")
    void shouldRejectManualRentedStatus() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));

        assertThatThrownBy(() -> carService.update(1L, CarRequestDto.builder()
                .model("Civic").status(CarStatus.RENTED).build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("RENTED");
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve mudar o status de carro com locação ativa")
    void shouldNotChangeStatusOfRentedCar() {
        car.setStatus(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> carService.update(1L, CarRequestDto.builder()
                .model("Civic").status(CarStatus.MAINTENANCE).build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("locação ativa");
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Carro alugado ainda pode ter dados de catálogo corrigidos")
    void shouldAllowCatalogEditOfRentedCar() {
        car.setStatus(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(rentalRepository.existsByCarIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(true);
        when(carRepository.save(any(CarModel.class))).thenAnswer(i -> i.getArgument(0));

        CarModel atualizado = carService.update(1L, CarRequestDto.builder()
                .model("Civic").color("Vermelho").plate("ABC-1D23").year(2024).build());

        assertThat(atualizado.getColor()).isEqualTo("Vermelho");
        assertThat(atualizado.getStatus()).isEqualTo(CarStatus.RENTED);
    }

    @Test
    @DisplayName("Editar carro inexistente deve retornar 404")
    void shouldReturnNotFoundOnUpdateOfMissingCar() {
        when(carRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.update(99L, CarRequestDto.builder().model("X").build()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
