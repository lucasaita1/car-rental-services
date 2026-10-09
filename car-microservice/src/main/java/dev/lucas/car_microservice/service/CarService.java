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

/**
 * Classe que encapsula a lógica de negócio para gerenciar carros.
 */
@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final RentalRepository rentalRepository;
    private final FileStorageService storage;

    /**
     * Persiste o modelo de carro no repositório.
     *
     * @param carModel o modelo de carro a ser salvo
     * @return o modelo de carro salvo com o ID gerado
     */
    public CarModel save(CarModel carModel){
        return carRepository.save(carModel);
    }

    /**
     * Recupera um carro pelo seu identificador único.
     *
     * @param id o identificador do carro
     * @return o modelo de carro encontrado
     * @throws RuntimeException se o carro não existir
     */
    public CarModel findById(Long id){
        return carRepository.findById(id).orElseThrow(() -> new RuntimeException("Car not found"));
    }

    /**
     * Atualiza os dados de um carro existente.
     * <p>
     * Valida que o status {@code RENTED} não pode ser alterado manualmente.
     * Também impede a mudança de status caso haja uma locação ativa para o carro.
     *
     * @param id  o identificador do carro a ser atualizado
     * @param dto os novos dados do carro
     * @return o modelo de carro atualizado
     * @throws ResponseStatusException se o status for {@code RENTED} ou se houver locação ativa
     */
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

    /**
     * Retorna todos os carros cadastrados.
     *
     * @return lista contendo todos os carros
     */
    public List<CarModel> findAll(){
        return carRepository.findAll();
    }

    /**
     * Exclui um carro pelo seu identificador.
     * <p>
     * Impede a exclusão se houver uma locação ativa associada ao carro.
     *
     * @param id o identificador do carro a ser excluído
     * @throws ResponseStatusException se houver locação ativa
     */
    public  void deleteById(Long id){
        if (rentalRepository.existsByCarIdAndStatus(id, RentalStatus.ACTIVE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Carro com locação ativa não pode ser removido.");
        }
        carRepository.findById(id).ifPresent(car -> storage.delete(car.getPhotoPath()));
        carRepository.deleteById(id);
    }

    /**
     * Atualiza a foto de um carro.
     * <p>
     * Salva a nova imagem, atualiza o caminho no modelo e remove a imagem antiga.
     *
     * @param id   o identificador do carro
     * @param file o arquivo de imagem a ser armazenado
     * @return o modelo de carro atualizado com o novo caminho da foto
     * @throws ResponseStatusException se o carro não existir
     */
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

    /**
     * Remove a foto de um carro.
     * <p>
     * Apaga o arquivo da foto e limpa o campo de caminho no modelo.
     *
     * @param id o identificador do carro
     * @return o modelo de carro atualizado sem foto
     * @throws ResponseStatusException se o carro não existir
     */
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
