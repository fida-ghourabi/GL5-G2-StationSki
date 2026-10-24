package com.example.gestionstationskii.repositories;

import com.example.gestionstationskii.entities.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository

public interface IRegistrationRepository extends org.springframework.data.jpa.repository.JpaRepository<Registration, Long> {
    List<Registration> findBySkierNumSkier(Long skierId);
    List<Registration> findByCourseNumCourse(Long courseId);
    List<Registration> findByPlanningSlotId(Long slotId);
    long countByPlanningSlotId(Long slotId);
    boolean existsBySkierNumSkier(Long skierId);
    boolean existsByCourseNumCourse(Long courseId);

    long countByCourseAndNumWeek(Course course, int numWeek);
    @Query("select distinct reg.numWeek from Instructor ins join ins.courses c, Registration reg " +
            "where reg.course = c and ins.numInstructor = :idIns and c.support = :support and reg.cancelled = false")
    List<Integer> numWeeksCourseOfInstructorBySupport(@Param("idIns") Long numInstructor, @Param("support") Support support);

    @Query("select count(distinct r) from Registration r " +
            "where r.numWeek = ?1 and r.skier.numSkier = ?2 and r.course.numCourse = ?3")
    long countDistinctByNumWeekAndSkierNumSkierAndCourseNumCourse(int numWeek, Long numSkier, Long numCourse);







}
