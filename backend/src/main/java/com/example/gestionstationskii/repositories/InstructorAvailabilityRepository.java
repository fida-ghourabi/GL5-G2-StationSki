package com.example.gestionstationskii.repositories;
import com.example.gestionstationskii.entities.InstructorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InstructorAvailabilityRepository extends JpaRepository<InstructorAvailability, Long> {
    List<InstructorAvailability> findByInstructor_NumInstructor(Long id);
    boolean existsByInstructor_NumInstructor(Long id);
}
