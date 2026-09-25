package dev.lucas.car_microservice.controller;

import dev.lucas.car_microservice.config.SecurityConfig;
import dev.lucas.car_microservice.dto.HoldResponse;
import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.security.TestJwt;
import dev.lucas.car_microservice.security.TokenRevocationChecker;
import dev.lucas.car_microservice.service.RentalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentalController.class)
@Import({SecurityConfig.class, TestJwt.Config.class})
class RentalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenRevocationChecker revocationChecker;

    @MockitoBean
    private RentalService rentalService;

    private RentalResponseDto locacao(Long userId) {
        return RentalResponseDto.builder()
                .id(100L).carId(1L).userId(userId).carPlate("ABC-1D23")
                .rentalDate(LocalDate.now()).status(RentalStatus.ACTIVE).build();
    }

    @Test
    @DisplayName("Alugar sem token retorna 401")
    void rentWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/rental/rent/1/user/5"))
                .andExpect(status().isUnauthorized());

        verify(rentalService, never()).rentCar(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("USER aluga para si mesmo")
    void userCanRentForSelf() throws Exception {
        when(rentalService.rentCar(1L, 5L, null)).thenReturn("Carro alugado com sucesso!");

        mockMvc.perform(post("/rental/rent/1/user/5").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk())
                .andExpect(content().string("Carro alugado com sucesso!"));
    }

    @Test
    @DisplayName("USER não aluga em nome de outro usuário")
    void userCannotRentForSomeoneElse() throws Exception {
        mockMvc.perform(post("/rental/rent/1/user/6").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());

        verify(rentalService, never()).rentCar(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("ADMIN aluga em nome de qualquer cliente")
    void adminCanRentForAnyone() throws Exception {
        when(rentalService.rentCar(1L, 6L, null)).thenReturn("Carro alugado com sucesso!");

        mockMvc.perform(post("/rental/rent/1/user/6").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Prazo de devolução é repassado ao serviço")
    void rentForwardsExpectedReturnDate() throws Exception {
        LocalDate prazo = LocalDate.now().plusDays(3);
        when(rentalService.rentCar(1L, 5L, prazo)).thenReturn("ok");

        mockMvc.perform(post("/rental/rent/1/user/5")
                        .param("expectedReturnDate", prazo.toString())
                        .header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk());

        verify(rentalService).rentCar(1L, 5L, prazo);
    }

    @Test
    @DisplayName("USER devolve o carro que ele mesmo alugou")
    void userCanReturnOwnRental() throws Exception {
        when(rentalService.findActiveRenterId(1L)).thenReturn(Optional.of(5L));
        when(rentalService.returnCar(1L)).thenReturn("Carro devolvido");

        mockMvc.perform(post("/rental/return/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("USER não devolve carro alugado por outra pessoa")
    void userCannotReturnSomeoneElsesRental() throws Exception {
        when(rentalService.findActiveRenterId(1L)).thenReturn(Optional.of(6L));

        mockMvc.perform(post("/rental/return/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());

        verify(rentalService, never()).returnCar(anyLong());
    }

    @Test
    @DisplayName("ADMIN devolve qualquer locação")
    void adminCanReturnAnyRental() throws Exception {
        when(rentalService.findActiveRenterId(1L)).thenReturn(Optional.of(6L));
        when(rentalService.returnCar(1L)).thenReturn("Carro devolvido");

        mockMvc.perform(post("/rental/return/1").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Devolução sem locação ativa chega ao serviço, que responde a recusa")
    void returnWithoutActiveRentalReachesService() throws Exception {
        when(rentalService.findActiveRenterId(1L)).thenReturn(Optional.empty());
        when(rentalService.returnCar(1L)).thenReturn("Carro já está disponível, não há aluguel ativo.");

        mockMvc.perform(post("/rental/return/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk())
                .andExpect(content().string("Carro já está disponível, não há aluguel ativo."));
    }

    @Test
    @DisplayName("USER vê o próprio histórico")
    void userCanSeeOwnHistory() throws Exception {
        when(rentalService.findRentalsByUser(5L)).thenReturn(List.of(locacao(5L)));

        mockMvc.perform(get("/rental/user/5").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("USER não vê o histórico de outro cliente")
    void userCannotSeeOthersHistory() throws Exception {
        mockMvc.perform(get("/rental/user/6").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Locações ativas são restritas ao ADMIN")
    void activeRentalsAreAdminOnly() throws Exception {
        when(rentalService.findActiveRentals()).thenReturn(List.of(locacao(5L)));

        mockMvc.perform(get("/rental/active").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rental/active").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("Locações vencidas são restritas ao ADMIN")
    void overdueRentalsAreAdminOnly() throws Exception {
        when(rentalService.findOverdueRentals()).thenReturn(List.of());

        mockMvc.perform(get("/rental/overdue").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rental/overdue").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Histórico por veículo é restrito ao ADMIN")
    void carHistoryIsAdminOnly() throws Exception {
        when(rentalService.findRentalsByCar(1L)).thenReturn(List.of(locacao(5L)));

        mockMvc.perform(get("/rental/car/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/rental/car/1").header("Authorization", TestJwt.admin()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Reservar exige login")
    void holdRequiresLogin() throws Exception {
        mockMvc.perform(post("/rental/hold/1")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Reserva é feita em nome do usuário do token")
    void holdUsesTokenUser() throws Exception {
        when(rentalService.holdCar(1L, 5L)).thenReturn(new HoldResponse(1L, java.time.Instant.parse("2026-09-25T20:10:00Z")));

        mockMvc.perform(post("/rental/hold/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carId").value(1))
                .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    @DisplayName("Carro reservado por outro retorna 409")
    void holdConflict() throws Exception {
        when(rentalService.holdCar(1L, 5L)).thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT, "Outro cliente está finalizando a locação deste carro."));

        mockMvc.perform(post("/rental/hold/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Cancelar a reserva solta o carro do próprio usuário")
    void releaseHold() throws Exception {
        mockMvc.perform(delete("/rental/hold/1").header("Authorization", TestJwt.user(5L)))
                .andExpect(status().isNoContent());

        verify(rentalService).releaseHold(1L, 5L);
    }
}
