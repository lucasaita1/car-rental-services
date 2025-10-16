package dev.lucas.car_microservice.service;


import dev.lucas.car_microservice.dto.RentalEmailDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final CarRepository carRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public String rentCar(Long carId, Long userId) {
        // Busca o carro pelo ID
        CarModel car = carRepository.findById(carId)
                .orElseThrow(() -> new RuntimeException("Carro não encontrado."));

        // Verifica se o carro está disponível
        if (car.getStatus() == CarStatus.RENTED) {
            return "Este carro já está alugado no momento.";
        }

        // Busca o cache do usuário no Redis
        String userKey = "user:" + userId;
        Object userCache = redisTemplate.opsForValue().get(userKey);

        if (userCache == null) {
            return "Usuário não encontrado no cache. É necessário fazer login novamente.";
        }

        // Converte o cache genérico retornado do Redis em um Map
        Map<String, Object> userData = (Map<String, Object>) userCache;

        // Atualiza as informações de aluguel do carro
        car.setRentalDate(LocalDate.now());
        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);

        // Cria DTO de e-mail com as informações do carro e do usuário
        RentalEmailDto emailRentalDto = new RentalEmailDto();
        emailRentalDto.setUserName((String) userData.get("name"));
        emailRentalDto.setUserEmail((String) userData.get("email"));
        emailRentalDto.setUserCpf((String) userData.get("cpf"));
        emailRentalDto.setCarModel(car.getModel());
        emailRentalDto.setCarPlate(car.getPlate());
        emailRentalDto.setRentalDate(car.getRentalDate());
        emailRentalDto.setReturnDate(car.getReturnDate());

        // Aqui futuramente será adicionado o envio da mensagem para o serviço de e-mail
        // por meio do RabbitMQ (Producer)

        return "Carro alugado com sucesso! Dados preparados para envio ao serviço de e-mail.";
    }

    public String returnCar(Long carId) {
        Optional<CarModel> optionalCar = carRepository.findById(carId);

        if (optionalCar.isEmpty()) {
            return "Carro não encontrado!";
        }

        CarModel car = optionalCar.get();

        if (car.getStatus() == CarStatus.AVAILABLE) {
            return "Carro já está disponível, não há aluguel ativo.";
        }

        car.setStatus(CarStatus.AVAILABLE);
        car.setRentalDate(null);
        carRepository.save(car);

        return "Carro devolvido e status atualizado para disponível.";
    }
}
