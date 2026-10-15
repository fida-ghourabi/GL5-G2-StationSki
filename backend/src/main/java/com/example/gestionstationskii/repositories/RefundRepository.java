package com.example.gestionstationskii.repositories;
import com.example.gestionstationskii.entities.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByPayment_Id(Long id);
}
