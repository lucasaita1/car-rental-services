package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.config.SecurityConfig;
import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.security.TestJwt;
import dev.lucas.car_microservice.security.TokenRevocationChecker;
import dev.lucas.car_microservice.service.CarService;
import dev.lucas.car_microservice.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarController.class)
@Import({SecurityConfig.class, TestJwt.Config.class})
class CarControllerTest {

    private static final String CAR_JSON =
            "{\"model\":\"Civic\",\"color\":\"Preto\",\"plate\":\"ABC-1D23\",\"year\":2024}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenRevocationChecker revocationChecker;

    @MockitoBean
    private CarService carService;

    @MockitoBean
    private ReservationService reservationService;

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
                null, null
        );
    }

    @Test
    @DisplayName("Deve criar um carro via POST /cars")
    void shouldCreateCar() throws Exception {
        when(carService.save(any(CarModel.class))).thenReturn(car);

        mockMvc.perform(post("/cars")
                        .header("Authorization", TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAR_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.model").value("Civic"))
                .andExpect(jsonPath("$.plate").value("ABC-1D23"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
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
        mockMvc.perform(delete("/cars/1").header("Authorization", TestJwt.admin()))
                .andExpect(status().isNoContent());

        verify(carService, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Deve editar um carro via PUT /cars/{id}")
    void shouldUpdateCar() throws Exception {
        car.setColor("Branco");
        when(carService.update(eq(1L), any(CarRequestDto.class))).thenReturn(car);

        mockMvc.perform(put("/cars/1")
                        .header("Authorization", TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"Civic\",\"color\":\"Branco\",\"plate\":\"ABC-1D23\",\"year\":2024}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("Branco"));
    }

    @Test
    @DisplayName("Catálogo é público mesmo com token inválido")
    void catalogIsPublicEvenWithGarbageToken() throws Exception {
        when(carService.findAll()).thenReturn(List.of(car));

        mockMvc.perform(get("/cars").header("Authorization", "Bearer lixo"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Criar carro sem token retorna 401")
    void createWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/cars").contentType(MediaType.APPLICATION_JSON).content(CAR_JSON))
                .andExpect(status().isUnauthorized());

        verify(carService, never()).save(any());
    }

    @Test
    @DisplayName("Criar carro como USER retorna 403")
    void createAsUserIsForbidden() throws Exception {
        mockMvc.perform(post("/cars")
                        .header("Authorization", TestJwt.user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAR_JSON))
                .andExpect(status().isForbidden());

        verify(carService, never()).save(any());
    }

    @Test
    @DisplayName("Editar carro como USER retorna 403")
    void updateAsUserIsForbidden() throws Exception {
        mockMvc.perform(put("/cars/1")
                        .header("Authorization", TestJwt.user(5L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAR_JSON))
                .andExpect(status().isForbidden());

        verify(carService, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("Remover carro como USER retorna 403")
    void deleteAsUserIsForbidden() throws Exception {
        mockMvc.perform(delete("/cars/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());

        verify(carService, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Token de admin vencido retorna 401")
    void expiredAdminTokenIsUnauthorized() throws Exception {
        String vencido = "Bearer " + TestJwt.token(1L, "ADMIN", Instant.now().minusSeconds(60), TestJwt.SECRET);

        mockMvc.perform(post("/cars")
                        .header("Authorization", vencido)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAR_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token de admin forjado com outro segredo retorna 401")
    void forgedAdminTokenIsUnauthorized() throws Exception {
        String forjado = "Bearer " + TestJwt.token(1L, "ADMIN", Instant.now().plusSeconds(3600), "outro-segredo");

        mockMvc.perform(post("/cars")
                        .header("Authorization", forjado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAR_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Preflight CORS do front-end é liberado")
    void corsPreflightFromFrontendIsAllowed() throws Exception {
        mockMvc.perform(options("/cars")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    @DisplayName("Preflight de origem desconhecida é recusado")
    void corsPreflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/cars")
                        .header("Origin", "http://site-malicioso.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token de admin revogado no logout retorna 401")
    void revokedAdminTokenIsUnauthorized() throws Exception {
        when(revocationChecker.isRevoked(any())).thenReturn(true);

        mockMvc.perform(post("/cars")
                        .header("Authorization", TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CAR_JSON))
                .andExpect(status().isUnauthorized());

        verify(carService, never()).save(any());
    }

    @Test
    @DisplayName("Placa inválida retorna 400 com a mensagem do campo")
    void invalidPlateIsRejected() throws Exception {
        mockMvc.perform(post("/cars")
                        .header("Authorization", TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"Civic\",\"color\":\"Preto\",\"plate\":\"PLACA1\",\"year\":2024}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Placa inválida. Use ABC-1234 ou ABC1D23."));

        verify(carService, never()).save(any());
    }

    @Test
    @DisplayName("Carro sem modelo e com ano fora da faixa retorna 400")
    void missingFieldsAreRejected() throws Exception {
        mockMvc.perform(put("/cars/1")
                        .header("Authorization", TestJwt.admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"Preto\",\"plate\":\"ABC1D23\",\"year\":1800}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.model").exists())
                .andExpect(jsonPath("$.errors.year").exists());

        verify(carService, never()).update(anyLong(), any());
    }

    @Test
    @DisplayName("ADMIN envia foto do carro e recebe a URL pública")
    void adminUploadsCarPhoto() throws Exception {
        car.setPhotoPath("cars/abc.jpg");
        when(carService.updatePhoto(eq(1L), any())).thenReturn(car);

        mockMvc.perform(multipart("/cars/1/photo")
                        .file(new MockMultipartFile("file", "civic.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                        .header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("/files/cars/abc.jpg"));
    }

    @Test
    @DisplayName("USER não envia foto de carro")
    void userCannotUploadCarPhoto() throws Exception {
        mockMvc.perform(multipart("/cars/1/photo")
                        .file(new MockMultipartFile("file", "civic.jpg", "image/jpeg", new byte[]{1}))
                        .header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());

        verify(carService, never()).updatePhoto(anyLong(), any());
    }

    @Test
    @DisplayName("ADMIN remove a foto do carro")
    void adminRemovesCarPhoto() throws Exception {
        when(carService.removePhoto(1L)).thenReturn(car);

        mockMvc.perform(delete("/cars/1/photo").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    @DisplayName("Catálogo marca carros reservados no checkout de outro cliente")
    void catalogFlagsReservedCars() throws Exception {
        when(carService.findAll()).thenReturn(List.of(car));
        when(reservationService.heldCarIds(List.of(1L))).thenReturn(java.util.Set.of(1L));

        mockMvc.perform(get("/cars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reserved").value(true));
    }
}
