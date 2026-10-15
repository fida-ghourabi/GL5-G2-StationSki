package com.example.gestionstationskii.repositories;
import com.example.gestionstationskii.entities.Rental;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RentalRepository extends JpaRepository<Rental, Long> {
    List<Rental> findBySkier_NumSkier(Long id);
    List<Rental> findByEquipment_Id(Long id);
    boolean existsBySkier_NumSkier(Long id);
    boolean existsByEquipment_Id(Long id);
}
