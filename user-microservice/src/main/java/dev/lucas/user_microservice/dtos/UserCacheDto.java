package dev.lucas.user_microservice.dtos;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheDto {
    private String id;
    private String name;
    private String cpf;
    private String email;
}