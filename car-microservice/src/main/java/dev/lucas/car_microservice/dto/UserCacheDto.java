package dev.lucas.car_microservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCacheDto {

    private String id;
    private String name;
    private String cpf;
    private String email;
}
