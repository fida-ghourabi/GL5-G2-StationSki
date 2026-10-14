package com.example.gestionstationskii.services;
import com.example.gestionstationskii.entities.*;
import java.util.List;

public interface IPisteServices {
    Piste updatePiste(Piste piste);
    List<Piste> search(Color color);
    java.util.Set<Skier> skiers(Long id);

    List<Piste> retrieveAllPistes();

    Piste  addPiste(Piste  piste);

    void removePiste (Long numPiste);

    Piste retrievePiste (Long numPiste);
}
