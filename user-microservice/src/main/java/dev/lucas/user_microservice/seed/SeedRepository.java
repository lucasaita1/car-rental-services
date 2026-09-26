package dev.lucas.user_microservice.seed;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeedRepository extends JpaRepository<SeedModel, String> {
}
