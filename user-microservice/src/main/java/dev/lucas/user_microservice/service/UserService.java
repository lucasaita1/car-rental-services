package dev.lucas.user_microservice.service;

import dev.lucas.user_microservice.entity.UserModel;
import dev.lucas.user_microservice.producer.UserProducer;
import dev.lucas.user_microservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        userModel.setPassword(passwordEncoder.encode(userModel.getPassword()));

        // O evento precisa ser publicado depois do save: antes disso o id ainda
        // não foi gerado pelo banco e o EmailDto sairia com userId nulo.
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
    public Optional<UserModel> updateById(Long id, UserModel user) {
        return userRepository.findById(id).map(existingUser -> {

            if (user.getName() != null && !user.getName().isBlank()) {
                existingUser.setName(user.getName());
            }

            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                existingUser.setEmail(user.getEmail());
            }

            if (user.getPassword() != null && !user.getPassword().isBlank()) {
                // A senha precisa passar pelo mesmo encoder usado no cadastro,
                // senão o BCrypt não casa no login e o usuário perde o acesso.
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            return existingUser;
        });
    }

    public void deleteById(Long id){
        userRepository.deleteById(id);
    }
}
