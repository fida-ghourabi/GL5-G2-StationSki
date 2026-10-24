package com.example.gestionstationskii.repositories;


import com.example.gestionstationskii.entities.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository

public interface IInstructorRepository extends JpaRepository<Instructor, Long> {
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from Instructor i where i.numInstructor = :id")
    java.util.Optional<Instructor> lockById(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<Instructor> findByCoursesNumCourse(Long courseId);

}
