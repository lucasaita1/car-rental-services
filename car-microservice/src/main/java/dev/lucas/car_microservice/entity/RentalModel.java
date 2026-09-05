package dev.lucas.car_microservice.entity;

import dev.lucas.car_microservice.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Registro durável de uma locação.
 *
 * <p>Esta é a fonte da verdade sobre aluguéis. As colunas de aluguel em
 * {@link CarModel} refletem apenas a locação corrente e são apagadas na
 * devolução; aqui a linha permanece, encerrada com status FINISHED.</p>
 *
 * <p>Os dados do cliente são gravados como cópia no momento da locação, e não
 * como referência. O usuário vive em outro microsserviço, com banco próprio,
 * então não existe join possível: sem a cópia, um relatório histórico
 * dependeria de chamar o user-microservice para cada linha.</p>
 */
@Entity
@Table(
        name = "TB_RENTALS",
        indexes = {
                @Index(name = "idx_rental_car_status", columnList = "carId,status"),
                @Index(name = "idx_rental_user", columnList = "userId")
        }
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RentalModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long carId;

    @Column(nullable = false)
    private Long userId;

    // Cópia dos dados do cliente no momento da locação.
    private String userName;
    private String userEmail;
    private String userCpf;

    // Cópia dos dados do veículo: DELETE /cars/{id} remove a linha do carro,
    // e sem isso o histórico ficaria sem saber qual veículo foi alugado.
    private String carModel;
    private String carPlate;

    @Column(nullable = false)
    private LocalDate rentalDate;

    /** Data combinada para a devolução. Nula quando o prazo não foi informado. */
    private LocalDate expectedReturnDate;

    /** Data em que o veículo voltou de fato. Nula enquanto a locação está ativa. */
    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    /** Locação em aberto, ou seja, veículo ainda com o cliente. */
    public boolean isActive() {
        return status == RentalStatus.ACTIVE;
    }

    /** Locação ativa cujo prazo combinado já passou. */
    public boolean isOverdue(LocalDate reference) {
        return isActive()
                && expectedReturnDate != null
                && expectedReturnDate.isBefore(reference);
    }
}
