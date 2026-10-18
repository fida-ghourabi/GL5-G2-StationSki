package com.example.gestionstationskii.repositories;

import com.example.gestionstationskii.entities.Piste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IPisteRepository extends JpaRepository<Piste, Long> {

}
