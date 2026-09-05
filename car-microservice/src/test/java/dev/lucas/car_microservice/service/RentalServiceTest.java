package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.entity.RentalModel;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import dev.lucas.car_microservice.repository.RentalRepository;
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
import java.util.List;
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
    private RentalRepository rentalRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RentalService rentalService;

    private CarModel availableCar;

    /** Locação em aberto do carro 1 pelo usuário 42. */
    private RentalModel locacaoAtiva() {
        RentalModel rental = new RentalModel();
        rental.setId(100L);
        rental.setCarId(1L);
        rental.setUserId(42L);
        rental.setUserName("Lucas");
        rental.setUserEmail("lucas@email.com");
        rental.setCarModel("Civic");
        rental.setCarPlate("ABC-1D23");
        rental.setRentalDate(LocalDate.now().minusDays(3));
        rental.setStatus(RentalStatus.ACTIVE);
        return rental;
    }

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
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        // A ocupação é decidida pela locação ativa, não mais pela coluna do carro.
        when(rentalRepository.existsByCarIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(true);

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("já está alugado");
        verify(carRepository, never()).save(any());
        verify(rentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve recusar aluguel mesmo se a coluna do carro estiver dessincronizada")
    void shouldTrustRentalTableOverCarColumn() {
        // Carro marcado como AVAILABLE, mas com locação em aberto na tabela.
        // Esse é o cenário que a coluna sozinha não conseguia detectar.
        availableCar.setStatus(CarStatus.AVAILABLE);
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(rentalRepository.existsByCarIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(true);

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("já está alugado");
        verify(rentalRepository, never()).save(any());
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
        when(rentalRepository.findByCarIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(Optional.of(locacaoAtiva()));

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
        when(rentalRepository.findByCarIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(Optional.empty());

        String result = rentalService.returnCar(1L);

        // A palavra "disponível" aparece nas duas mensagens do fluxo de devolução,
        // então a asserção precisa ser sobre o trecho que só existe na recusa.
        assertThat(result).contains("não há aluguel ativo");
        assertThat(availableCar.getStatus()).isEqualTo(CarStatus.AVAILABLE);
        verify(carRepository, never()).save(any());
        verify(rentalRepository, never()).save(any());
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
        when(rentalRepository.findByCarIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(Optional.of(locacaoAtiva()));

        rentalService.returnCar(1L);

        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository).save(captor.capture());

        // Um carro disponível não pode continuar apontando para o locatário anterior.
        assertThat(captor.getValue().getUserId()).isNull();
    }

    @Test
    @DisplayName("Aluguel deve espelhar no carro o prazo combinado na locação")
    void shouldMirrorExpectedReturnDateOnCar() {
        LocalDate prazo = LocalDate.now().plusDays(10);
        // Valor antigo na coluna do carro, de uma locação anterior.
        availableCar.setReturnDate(LocalDate.of(2020, 1, 1));
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42")).thenReturn(Map.of("name", "Lucas", "email", "l@x.com", "cpf", "1"));

        rentalService.rentCar(1L, 42L, prazo);

        ArgumentCaptor<CarModel> captor = ArgumentCaptor.forClass(CarModel.class);
        verify(carRepository).save(captor.capture());

        // O prazo agora pertence à locação; a coluna do carro é só o espelho da
        // locação corrente e não pode carregar resíduo da anterior.
        assertThat(captor.getValue().getReturnDate()).isEqualTo(prazo);
    }

    // ------------------------------------------------------------------
    // Registro durável da locação em TB_RENTALS
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Deve criar a locação com os dados do cliente e do veículo")
    void shouldPersistRentalRecord() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42"))
                .thenReturn(Map.of("name", "Lucas", "email", "lucas@x.com", "cpf", "12345678900"));

        rentalService.rentCar(1L, 42L, LocalDate.now().plusDays(5));

        ArgumentCaptor<RentalModel> captor = ArgumentCaptor.forClass(RentalModel.class);
        verify(rentalRepository).save(captor.capture());

        RentalModel rental = captor.getValue();
        assertThat(rental.getCarId()).isEqualTo(1L);
        assertThat(rental.getUserId()).isEqualTo(42L);
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(rental.getRentalDate()).isEqualTo(LocalDate.now());
        assertThat(rental.getExpectedReturnDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(rental.getReturnDate()).isNull();
    }

    @Test
    @DisplayName("Deve copiar os dados do cliente, que vivem em outro microsserviço")
    void shouldSnapshotUserDataOnRental() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42"))
                .thenReturn(Map.of("name", "Lucas", "email", "lucas@x.com", "cpf", "12345678900"));

        rentalService.rentCar(1L, 42L);

        ArgumentCaptor<RentalModel> captor = ArgumentCaptor.forClass(RentalModel.class);
        verify(rentalRepository).save(captor.capture());

        // Sem a cópia, montar um relatório histórico exigiria uma chamada ao
        // user-microservice por linha, e o dado sumiria se o cliente fosse removido.
        RentalModel rental = captor.getValue();
        assertThat(rental.getUserName()).isEqualTo("Lucas");
        assertThat(rental.getUserEmail()).isEqualTo("lucas@x.com");
        assertThat(rental.getUserCpf()).isEqualTo("12345678900");
        assertThat(rental.getCarModel()).isEqualTo("Civic");
        assertThat(rental.getCarPlate()).isEqualTo("ABC-1D23");
    }

    @Test
    @DisplayName("Devolução deve encerrar a locação sem apagar o histórico")
    void shouldCloseRentalWithoutDeleting() {
        availableCar.setStatus(CarStatus.RENTED);
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(rentalRepository.findByCarIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(Optional.of(locacaoAtiva()));

        rentalService.returnCar(1L);

        ArgumentCaptor<RentalModel> captor = ArgumentCaptor.forClass(RentalModel.class);
        verify(rentalRepository).save(captor.capture());

        RentalModel encerrada = captor.getValue();
        assertThat(encerrada.getStatus()).isEqualTo(RentalStatus.FINISHED);
        assertThat(encerrada.getReturnDate()).isEqualTo(LocalDate.now());
        // A data original da locação continua registrada: é isso que o modelo
        // anterior perdia, porque zerava a coluna no carro.
        assertThat(encerrada.getRentalDate()).isEqualTo(LocalDate.now().minusDays(3));
        verify(rentalRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Não deve aceitar prazo de devolução no passado")
    void shouldRejectPastExpectedReturnDate() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));

        String result = rentalService.rentCar(1L, 42L, LocalDate.now().minusDays(1));

        assertThat(result).contains("não pode ser anterior a hoje");
        verify(rentalRepository, never()).save(any());
        verify(carRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aceitar locação sem prazo combinado")
    void shouldAllowRentalWithoutDeadline() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42")).thenReturn(Map.of("name", "Lucas", "email", "l@x.com", "cpf", "1"));

        String result = rentalService.rentCar(1L, 42L);

        assertThat(result).contains("sucesso");

        ArgumentCaptor<RentalModel> captor = ArgumentCaptor.forClass(RentalModel.class);
        verify(rentalRepository).save(captor.capture());
        assertThat(captor.getValue().getExpectedReturnDate()).isNull();
    }

    @Test
    @DisplayName("Não deve gravar locação quando o usuário não está no cache")
    void shouldNotPersistRentalWithoutUserData() {
        when(carRepository.findById(1L)).thenReturn(Optional.of(availableCar));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user:42")).thenReturn(null);

        rentalService.rentCar(1L, 42L);

        verify(rentalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar o histórico do cliente da locação mais recente para a mais antiga")
    void shouldListRentalsByUser() {
        when(rentalRepository.findByUserIdOrderByRentalDateDesc(42L))
                .thenReturn(List.of(locacaoAtiva()));

        List<RentalResponseDto> historico = rentalService.findRentalsByUser(42L);

        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getUserId()).isEqualTo(42L);
        assertThat(historico.get(0).getCarPlate()).isEqualTo("ABC-1D23");
    }

    @Test
    @DisplayName("Consulta não deve expor o CPF do cliente")
    void shouldNotExposeCpfInQueries() {
        when(rentalRepository.findByStatus(RentalStatus.ACTIVE))
                .thenReturn(List.of(locacaoAtiva()));

        List<RentalResponseDto> ativas = rentalService.findActiveRentals();

        // O DTO de resposta simplesmente não tem o campo; o CPF fica restrito
        // ao registro interno e ao e-mail de confirmação.
        assertThat(ativas).hasSize(1);
        assertThat(ativas.get(0).getUserName()).isEqualTo("Lucas");
    }

    @Test
    @DisplayName("Deve marcar como vencida a locação cujo prazo já passou")
    void shouldFlagOverdueRentals() {
        RentalModel atrasada = locacaoAtiva();
        atrasada.setExpectedReturnDate(LocalDate.now().minusDays(2));
        when(rentalRepository.findOverdue(LocalDate.now())).thenReturn(List.of(atrasada));

        List<RentalResponseDto> vencidas = rentalService.findOverdueRentals();

        assertThat(vencidas).hasSize(1);
        assertThat(vencidas.get(0).isOverdue()).isTrue();
    }

    @Test
    @DisplayName("Locação dentro do prazo não deve ser marcada como vencida")
    void shouldNotFlagRentalWithinDeadline() {
        RentalModel emDia = locacaoAtiva();
        emDia.setExpectedReturnDate(LocalDate.now().plusDays(2));
        when(rentalRepository.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of(emDia));

        List<RentalResponseDto> ativas = rentalService.findActiveRentals();

        assertThat(ativas.get(0).isOverdue()).isFalse();
    }
}