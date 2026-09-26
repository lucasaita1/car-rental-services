package dev.lucas.car_microservice.entity;

import dev.lucas.car_microservice.enums.CarStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CarModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String model;
    private String color;
    private String plate;
    private int year;

    @Column(precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @JdbcTypeCode(SqlTypes.JSON)
    private List<CarDetail> details;

    private LocalDate rentalDate;
    private LocalDate returnDate;
    @Enumerated(EnumType.STRING)
    private CarStatus status;
    private Long userId;
    private String photoPath;

}
