package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.dtos.ProfileUpdateRequest;
import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.enums.UserRole;
import dev.lucas.user_microservice.producer.UserProducer;
import dev.lucas.user_microservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserProducer userProducer;

    @Transactional
    public UserModel saveUser(UserModel userModel){
        if (userRepository.existsByEmail(userModel.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));

        UserModel savedUser = userRepository.save(userModel);
        userProducer.sendRegisterEmail(savedUser);
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

    public void deleteById(Long id){
        userRepository.deleteById(id);
    }
}
