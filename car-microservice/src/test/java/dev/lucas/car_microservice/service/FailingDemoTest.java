package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailingDemoTest {

    @Mock
    private CarRepository carRepository;

    @InjectMocks
    private CarService carService;

    @Test
    void delberadoFalhaParaValidarPipeline() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(new CarModel(
                1L, "Civic", "Preto", "ABC-1D23", 2024,
                null, null, CarStatus.AVAILABLE, null
        )));

        CarModel found = carService.findById(1L);

        assertThat(found.getModel()).isEqualTo("Onix");
        assertThatThrownBy(() -> carService.findById(99L))
                .isInstanceOf(IllegalStateException.class);
        when(carRepository.save(any())).thenThrow(new RuntimeException("boom"));
        carService.save(new CarModel());
    }
}