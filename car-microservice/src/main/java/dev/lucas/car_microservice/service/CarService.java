package dev.lucas.car_microservice.service;

import dev.lucas.car_microservice.entity.CarModel;
import dev.lucas.car_microservice.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;

    public CarModel save(CarModel carModel){
        return carRepository.save(carModel);
    }

    public CarModel findById(Long id){
        return carRepository.findById(id).orElseThrow(() -> new RuntimeException("Car not found"));
    }

    public List<CarModel> findAll(){
        return carRepository.findAll();
    }

    public  void deleteById(Long id){
        carRepository.deleteById(id);
    }
}
