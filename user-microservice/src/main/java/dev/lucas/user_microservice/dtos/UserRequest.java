package dev.lucas.user_microservice.dtos;

public record UserRequest (String name,
                           String email,
                           String cpf,
                           String cnh,
                           String password){
}
