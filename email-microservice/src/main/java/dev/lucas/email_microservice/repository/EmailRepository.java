package dev.lucas.email_microservice.repository;

import dev.lucas.email_microservice.entity.Email;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailRepository extends MongoRepository<Email, String> {
}
