package com.example.gestionstationskii.repositories;


import com.example.gestionstationskii.entities.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICourseRepository extends JpaRepository<Course, Long> {
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select c from Course c where c.numCourse = :id")
    java.util.Optional<Course> lockById(@org.springframework.data.repository.query.Param("id") Long id);



}
