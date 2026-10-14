package com.example.gestionstationskii.repositories;
import com.example.gestionstationskii.entities.Equipment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    @Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> lockById(@Param("id") Long id);
    boolean existsByInventoryCode(String code);
}
