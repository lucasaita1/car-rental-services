package dev.lucas.user_microservice.seed;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "TB_SEEDS")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeedModel {

    @Id
    private String name;
    private Instant executedAt;
}
