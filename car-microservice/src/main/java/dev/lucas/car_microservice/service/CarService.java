package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import dev.lucas.car_microservice.repository.RentalRepository;
import dev.lucas.car_microservice.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;

    public CarModel save(CarModel carModel){
        return carRepository.save(carModel);
    }

    public CarModel findById(Long id){
        return carRepository.findById(id).orElseThrow(() -> new RuntimeException("Car not found"));
    }

    @Transactional
    public CarModel update(Long id, CarRequestDto dto) {
        CarModel car = carRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carro não encontrado."));

        if (dto.getStatus() == CarStatus.RENTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O status RENTED é definido pelo aluguel, não pela edição.");
        }

        boolean rented = rentalRepository.existsByCarIdAndStatus(id, RentalStatus.ACTIVE);
        if (rented && dto.getStatus() != null && dto.getStatus() != car.getStatus()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Carro com locação ativa: registre a devolução antes de mudar o status.");
        }

        car.setModel(InputSanitizer.text(dto.getModel()));
        car.setColor(InputSanitizer.text(dto.getColor()));
        car.setPlate(InputSanitizer.plate(dto.getPlate()));
        car.setYear(dto.getYear());
        if (dto.getStatus() != null) {
            car.setStatus(dto.getStatus());
        }
        return carRepository.save(car);
    }

    public List<CarModel> findAll(){
        return carRepository.findAll();
    }

    public  void deleteById(Long id){
        if (rentalRepository.existsByCarIdAndStatus(id, RentalStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Carro com locação ativa não pode ser removido.");
        }
        carRepository.deleteById(id);
    }
}
