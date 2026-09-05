package dev.lucas.car_microservice.repository;

import dev.lucas.car_microservice.entity.RentalModel;
import dev.lucas.car_microservice.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RentalRepository extends JpaRepository<RentalModel, Long> {

    /** Locação em aberto de um veículo. Só pode existir uma por vez. */
    Optional<RentalModel> findByCarIdAndStatus(Long carId, RentalStatus status);

    /** Histórico de um cliente, da locação mais recente para a mais antiga. */
    List<RentalModel> findByUserIdOrderByRentalDateDesc(Long userId);

    /** Histórico de um veículo, da locação mais recente para a mais antiga. */
    List<RentalModel> findByCarIdOrderByRentalDateDesc(Long carId);

    List<RentalModel> findByStatus(RentalStatus status);

    /** Locações ativas cujo prazo combinado já venceu. */
    @Query("""
            SELECT r FROM RentalModel r
            WHERE r.status = dev.lucas.car_microservice.enums.RentalStatus.ACTIVE
              AND r.expectedReturnDate IS NOT NULL
              AND r.expectedReturnDate < :reference
            ORDER BY r.expectedReturnDate ASC
            """)
    List<RentalModel> findOverdue(@Param("reference") LocalDate reference);

    boolean existsByCarIdAndStatus(Long carId, RentalStatus status);
}
