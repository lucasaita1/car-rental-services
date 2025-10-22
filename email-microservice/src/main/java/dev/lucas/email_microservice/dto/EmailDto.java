package dev.lucas.email_microservice.dto;

public record EmailDto(String userId,
                       String emailTo,
                       String subject,
                       String text){
}
