package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.service.CarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Disabled("Desabilitado temporariamente para demonstrar o Quality Gate bloqueando (Aula 3).")
@WebMvcTest(CarController.class)
class CarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
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
    @DisplayName("Deve criar um carro via POST /cars")
    void shouldCreateCar() throws Exception {
        when(carService.save(any(CarModel.class))).thenReturn(car);

        mockMvc.perform(post("/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"Civic\",\"color\":\"Preto\",\"plate\":\"ABC-1D23\",\"year\":2024}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.model").value("Civic"))
                .andExpect(jsonPath("$.plate").value("ABC-1D23"));
    }

    @Test
    @DisplayName("Deve buscar um carro por id via GET /cars/{id}")
    void shouldGetCarById() throws Exception {
        when(carService.findById(1L)).thenReturn(car);

        mockMvc.perform(get("/cars/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.model").value("Civic"));
    }

    @Test
    @DisplayName("Deve listar todos os carros via GET /cars")
    void shouldGetAllCars() throws Exception {
        when(carService.findAll()).thenReturn(List.of(car));

        mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].plate").value("ABC-1D23"));
    }

    @Test
    @DisplayName("Deve remover um carro via DELETE /cars/{id}")
    void shouldDeleteCar() throws Exception {
        mockMvc.perform(delete("/cars/1"))
                .andExpect(status().isNoContent());

        verify(carService, times(1)).deleteById(1L);
    }
}
