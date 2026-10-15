package com.example.gestionstationskii.repositories;
import com.example.gestionstationskii.entities.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> lockById(@Param("id") Long id);
    List<Payment> findBySkier_NumSkier(Long id);
    List<Payment> findBySubscription_NumSub(Long id);
    boolean existsBySkier_NumSkier(Long id);
    boolean existsBySubscription_NumSub(Long id);
    boolean existsByTransactionReference(String reference);
}
