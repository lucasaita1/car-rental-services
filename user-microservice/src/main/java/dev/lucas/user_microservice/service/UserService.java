package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.dtos.ProfileUpdateRequest;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.producer.UserProducer;
import dev.lucas.user_microservice.repository.UserRepository;
import dev.lucas.user_microservice.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    public static final String PHOTO_FOLDER = "users";
    public static final String CNH_FOLDER = "documents/cnh";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProducer userProducer;
    private final FileStorageService storage;

    @Transactional
    public UserModel saveUser(UserModel userModel){
        if (userRepository.existsByEmail(userModel.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));

        UserModel savedUser = userRepository.save(userModel);
        try {
            userProducer.sendRegisterEmail(savedUser);
        } catch (AmqpException e) {
            log.warn("E-mail de boas-vindas não enviado para o usuário {}: {}", savedUser.getId(), e.getMessage());
        }
        return savedUser;
    }

    public List<UserModel> getAllUsers(){
        return userRepository.findAll();
    }

    public Optional<UserModel> getUserById(Long id){
        return userRepository.findById(id);
    }

    @Transactional
    public Optional<UserModel> updateProfile(Long id, ProfileUpdateRequest request) {
        return userRepository.findById(id).map(user -> {
            if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
            }
            user.setName(request.name());
            user.setEmail(request.email());
            user.setCpf(request.cpf());
            user.setCnh(request.cnh());
            return userRepository.save(user);
        });
    }

    @Transactional
    public Optional<UserModel> changeRole(Long id, UserRole role) {
        return userRepository.findById(id).map(user -> {
            user.setRole(role);
            return userRepository.save(user);
        });
    }

    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        UserModel user = findExisting(id);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public UserModel updatePhoto(Long id, MultipartFile file) {
        UserModel user = findExisting(id);
        String newPath = storage.storeImage(file, PHOTO_FOLDER);
        String oldPath = user.getPhotoPath();
        user.setPhotoPath(newPath);
        UserModel saved = userRepository.save(user);
        storage.delete(oldPath);
        return saved;
    }

    @Transactional
    public UserModel removePhoto(Long id) {
        UserModel user = findExisting(id);
        storage.delete(user.getPhotoPath());
        user.setPhotoPath(null);
        return userRepository.save(user);
    }

    @Transactional
    public UserModel updateCnhDocument(Long id, MultipartFile file) {
        UserModel user = findExisting(id);
        String newPath = storage.storePdf(file, CNH_FOLDER);
        String oldPath = user.getCnhDocumentPath();
        user.setCnhDocumentPath(newPath);
        user.setCnhDocumentUploadedAt(Instant.now());
        UserModel saved = userRepository.save(user);
        storage.delete(oldPath);
        return saved;
    }

    public Resource cnhDocument(Long id) {
        UserModel user = findExisting(id);
        if (!user.hasCnhDocument()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CNH ainda não enviada.");
        }
        return storage.load(user.getCnhDocumentPath());
    }

    public void deleteById(Long id){
        userRepository.findById(id).ifPresent(user -> {
            storage.delete(user.getPhotoPath());
            storage.delete(user.getCnhDocumentPath());
        });
        userRepository.deleteById(id);
    }

    private UserModel findExisting(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }
}
