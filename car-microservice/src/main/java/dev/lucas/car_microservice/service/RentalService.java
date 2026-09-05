package dev.lucas.car_microservice.service;


import dev.lucas.car_microservice.dto.RentalEmailDto;
import dev.lucas.car_microservice.dto.RentalResponseDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.entity.RentalModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.mapper.RentalMapper;
import dev.lucas.car_microservice.repository.CarRepository;
import dev.lucas.car_microservice.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public String rentCar(Long carId, Long userId) {
        return rentCar(carId, userId, null);
    }

    @Transactional
    public String rentCar(Long carId, Long userId, LocalDate expectedReturnDate) {
        CarModel car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Carro não encontrado."));

        if (car.getStatus() == CarStatus.MAINTENANCE) {
            return "Este carro está em manutenção e não pode ser alugado.";
        }

        // Quem decide se o veículo está ocupado é a tabela de locações. A coluna
        // status do carro é só um espelho e pode ficar dessincronizada.
        if (rentalRepository.existsByCarIdAndStatus(carId, RentalStatus.ACTIVE)) {
            return "Este carro já está alugado no momento.";
        }

        LocalDate hoje = LocalDate.now();

        if (expectedReturnDate != null && expectedReturnDate.isBefore(hoje)) {
            return "A data prevista de devolução não pode ser anterior a hoje.";
        }

        String userKey = "user:" + userId;
        Object userCache = redisTemplate.opsForValue().get(userKey);

        if (userCache == null) {
            return "Usuário não encontrado no cache. É necessário fazer login novamente.";
        }

        Map<String, Object> userData = (Map<String, Object>) userCache;

        // Registro durável da locação.
        RentalModel rental = new RentalModel();
        rental.setCarId(car.getId());
        rental.setUserId(userId);
        rental.setUserName((String) userData.get("name"));
        rental.setUserEmail((String) userData.get("email"));
        rental.setUserCpf((String) userData.get("cpf"));
        rental.setCarModel(car.getModel());
        rental.setCarPlate(car.getPlate());
        rental.setRentalDate(hoje);
        rental.setExpectedReturnDate(expectedReturnDate);
        rental.setStatus(RentalStatus.ACTIVE);
        rentalRepository.save(rental);

        // Espelho da locação corrente no veículo, para consulta rápida de estoque.
        car.setRentalDate(hoje);
        car.setReturnDate(expectedReturnDate);
        car.setStatus(CarStatus.RENTED);
        car.setUserId(userId);
        carRepository.save(car);

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

    private List<RentalResponseDto> toDtoList(List<RentalModel> rentals) {
        LocalDate hoje = LocalDate.now();
        return rentals.stream()
                .map(rental -> RentalMapper.toResponseDto(rental, hoje))
                .toList();
    }
}
