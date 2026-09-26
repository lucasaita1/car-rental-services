package dev.lucas.car_microservice.service;


import dev.lucas.car_microservice.dto.HoldResponse;
import dev.lucas.car_microservice.dto.RentalEmailDto;
import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.dto.UserCacheDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.entity.RentalModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.mapper.RentalMapper;
import dev.lucas.car_microservice.repository.CarRepository;
import dev.lucas.car_microservice.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Regras de locação.
 *
 * <p>Cada aluguel gera uma linha em TB_RENTALS, que é a fonte da verdade e
 * sobrevive à devolução. O Redis participa apenas como cache de leitura dos
 * dados do cliente: se a chave expirar, perde-se a conveniência, nunca o
 * registro da locação.</p>
 */
@Service
@RequiredArgsConstructor
public class RentalService {

    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ReservationService reservationService;

    static final String CNH_REQUIRED = "Envie o PDF da sua CNH no perfil antes de alugar.";

    @Transactional
    public String rentCar(Long carId, Long userId, LocalDate expectedReturnDate) {
        CarModel car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Carro não encontrado."));

        if (car.getStatus() == CarStatus.MAINTENANCE) {
            return "Este carro está em manutenção e não pode ser alugado.";
        }

        if (car.getDailyRate() == null) {
            return "Este carro ainda não tem valor de diária definido.";
        }

        // Quem decide se o veículo está ocupado é a tabela de locações. A coluna
        // status do carro é só um espelho e pode ficar dessincronizada.
        if (rentalRepository.existsByCarIdAndStatus(carId, RentalStatus.ACTIVE)) {
            return "Este carro já está alugado no momento.";
        }

        LocalDate hoje = LocalDate.now();

        if (expectedReturnDate == null) {
            return "Informe a data prevista de devolução.";
        }

        if (expectedReturnDate.isBefore(hoje)) {
            return "A data prevista de devolução não pode ser anterior a hoje.";
        }

        if (reservationService.hold(carId, userId).isPresent()) {
            return "Este carro está reservado por outro cliente no momento.";
        }

        Optional<UserCacheDto> cached = cachedUser(userId);

        if (cached.isEmpty()) {
            reservationService.release(carId, userId);
            return "Usuário não encontrado no cache. É necessário fazer login novamente.";
        }

        UserCacheDto userData = cached.get();

        if (!userData.isCnhDocument()) {
            reservationService.release(carId, userId);
            return CNH_REQUIRED;
        }

        // Registro durável da locação.
        RentalModel rental = new RentalModel();
        rental.setCarId(car.getId());
        rental.setUserId(userId);
        rental.setUserName(userData.getName());
        rental.setUserEmail(userData.getEmail());
        rental.setUserCpf(userData.getCpf());
        rental.setCarModel(car.getModel());
        rental.setCarPlate(car.getPlate());
        rental.setRentalDate(hoje);
        rental.setExpectedReturnDate(expectedReturnDate);
        rental.setDailyRate(car.getDailyRate());
        rental.setEstimatedTotal(RentalPricing.total(car.getDailyRate(), hoje, expectedReturnDate));
        rental.setStatus(RentalStatus.ACTIVE);
        rentalRepository.save(rental);

        // Espelho da locação corrente no veículo, para consulta rápida de estoque.
        car.setRentalDate(hoje);
        car.setReturnDate(expectedReturnDate);
        car.setStatus(CarStatus.RENTED);
        car.setUserId(userId);
        carRepository.save(car);
        reservationService.release(carId, userId);

        RentalEmailDto emailRentalDto = new RentalEmailDto();
        emailRentalDto.setUserName(rental.getUserName());
        emailRentalDto.setUserEmail(rental.getUserEmail());
        emailRentalDto.setUserCpf(rental.getUserCpf());
        emailRentalDto.setCarModel(rental.getCarModel());
        emailRentalDto.setCarPlate(rental.getCarPlate());
        emailRentalDto.setRentalDate(rental.getRentalDate());
        emailRentalDto.setReturnDate(rental.getExpectedReturnDate());

        // Aqui futuramente será adicionado o envio da mensagem para o serviço de e-mail
        // por meio do RabbitMQ (Producer)

        return "Carro alugado com sucesso! Dados preparados para envio ao serviço de e-mail.";
    }

    @Transactional
    public String returnCar(Long carId) {
        Optional<CarModel> optionalCar = carRepository.findById(carId);

        if (optionalCar.isEmpty()) {
            return "Carro não encontrado!";
        }

        Optional<RentalModel> optionalRental =
                rentalRepository.findByCarIdAndStatus(carId, RentalStatus.ACTIVE);

        if (optionalRental.isEmpty()) {
            return "Carro já está disponível, não há aluguel ativo.";
        }

        // Encerra a locação sem apagá-la: a linha permanece como histórico.
        RentalModel rental = optionalRental.get();
        rental.setReturnDate(LocalDate.now());
        rental.setTotalAmount(RentalPricing.total(rental.getDailyRate(), rental.getRentalDate(), rental.getReturnDate()));
        rental.setStatus(RentalStatus.FINISHED);
        rentalRepository.save(rental);

        CarModel car = optionalCar.get();
        car.setStatus(CarStatus.AVAILABLE);
        car.setRentalDate(null);
        car.setReturnDate(null);
        car.setUserId(null);
        carRepository.save(car);

        return "Carro devolvido e status atualizado para disponível.";
    }

    public HoldResponse holdCar(Long carId, Long userId) {
        CarModel car = carRepository.findById(carId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carro não encontrado."));

        if (car.getStatus() == CarStatus.MAINTENANCE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este carro está em manutenção.");
        }
        if (car.getDailyRate() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este carro ainda não tem valor de diária definido.");
        }
        if (rentalRepository.existsByCarIdAndStatus(carId, RentalStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este carro já está alugado.");
        }
        if (cachedUser(userId).filter(user -> !user.isCnhDocument()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, CNH_REQUIRED);
        }
        if (reservationService.hold(carId, userId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Outro cliente está finalizando a locação deste carro. Tente novamente em alguns minutos.");
        }
        return new HoldResponse(carId, reservationService.expiresAt(carId));
    }

    public void releaseHold(Long carId, Long userId) {
        reservationService.release(carId, userId);
    }

    public Optional<Long> findActiveRenterId(Long carId) {
        return rentalRepository.findByCarIdAndStatus(carId, RentalStatus.ACTIVE)
                .map(RentalModel::getUserId);
    }

    /** Histórico completo de locações de um cliente. */
    public List<RentalResponseDto> findRentalsByUser(Long userId) {
        return toDtoList(rentalRepository.findByUserIdOrderByRentalDateDesc(userId));
    }

    /** Histórico completo de locações de um veículo. */
    public List<RentalResponseDto> findRentalsByCar(Long carId) {
        return toDtoList(rentalRepository.findByCarIdOrderByRentalDateDesc(carId));
    }

    /** Locações em aberto no momento. */
    public List<RentalResponseDto> findActiveRentals() {
        return toDtoList(rentalRepository.findByStatus(RentalStatus.ACTIVE));
    }

    /** Locações ativas cujo prazo combinado já venceu. */
    public List<RentalResponseDto> findOverdueRentals() {
        return toDtoList(rentalRepository.findOverdue(LocalDate.now()));
    }

    private Optional<UserCacheDto> cachedUser(Long userId) {
        Object value = redisTemplate.opsForValue().get("user:" + userId);
        if (value instanceof UserCacheDto user) {
            return Optional.of(user);
        }
        if (value instanceof Map<?, ?> map) {
            return Optional.of(new UserCacheDto(
                    asText(map.get("id")),
                    asText(map.get("name")),
                    asText(map.get("cpf")),
                    asText(map.get("email")),
                    Boolean.TRUE.equals(map.get("cnhDocument"))));
        }
        return Optional.empty();
    }

    private static String asText(Object value) {
        return value == null ? null : value.toString();
    }

    private List<RentalResponseDto> toDtoList(List<RentalModel> rentals) {
        LocalDate hoje = LocalDate.now();
        return rentals.stream()
                .map(rental -> RentalMapper.toResponseDto(rental, hoje))
                .toList();
    }
}
