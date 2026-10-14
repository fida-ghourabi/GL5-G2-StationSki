package com.example.gestionstationskii.repositories;


import com.example.gestionstationskii.entities.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository

public interface IInstructorRepository extends JpaRepository<Instructor, Long> {

}
