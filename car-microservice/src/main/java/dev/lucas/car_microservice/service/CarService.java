package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.dto.CarRequestDto;
import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.mapper.CarMapper;
import dev.lucas.car_microservice.enums.CarStatus;
import dev.lucas.car_microservice.enums.RentalStatus;
import dev.lucas.car_microservice.repository.CarRepository;
import dev.lucas.car_microservice.repository.RentalRepository;
import dev.lucas.car_microservice.storage.FileStorageService;
import dev.lucas.car_microservice.util.InputSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;
    private final FileStorageService storage;

    public CarModel save(CarModel carModel){
        return carRepository.save(carModel);
    }

    public CarModel findById(Long id){
        return carRepository.findById(id).orElseThrow(() -> new RuntimeException("Car not found"));
    }

    @Transactional
    public CarModel update(Long id, CarRequestDto dto) {
        CarModel car = findExisting(id);

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
        car.setDailyRate(dto.getDailyRate());
        car.setDetails(CarMapper.sanitizeDetails(dto.getDetails()));
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
        carRepository.findById(id).ifPresent(car -> storage.delete(car.getPhotoPath()));
        carRepository.deleteById(id);
    }

    @Transactional
    public CarModel updatePhoto(Long id, MultipartFile file) {
        CarModel car = findExisting(id);
        String newPath = storage.storeImage(file, "cars");
        String oldPath = car.getPhotoPath();
        car.setPhotoPath(newPath);
        CarModel saved = carRepository.save(car);
        storage.delete(oldPath);
        return saved;
    }

    @Transactional
    public CarModel removePhoto(Long id) {
        CarModel car = findExisting(id);
        storage.delete(car.getPhotoPath());
        car.setPhotoPath(null);
        return carRepository.save(car);
    }

    private CarModel findExisting(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Carro não encontrado."));
    }
}
