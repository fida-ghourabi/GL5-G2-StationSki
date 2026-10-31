package com.example.gestionstationskii.repositories;
import com.example.gestionstationskii.entities.PlanningSlot;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.Optional;
public interface PlanningSlotRepository extends JpaRepository<PlanningSlot, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from PlanningSlot s where s.id = :id")
    Optional<PlanningSlot> lockById(@Param("id") Long id);
    boolean existsByCourseNumCourse(Long id);
    boolean existsByInstructorNumInstructor(Long id);
}
