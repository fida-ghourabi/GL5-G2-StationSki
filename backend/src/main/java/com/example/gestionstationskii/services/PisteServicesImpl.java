package com.example.gestionstationskii.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import java.util.List;
@RequiredArgsConstructor
@Service
@Transactional
public class PisteServicesImpl implements IPisteServices {

    private final IPisteRepository pisteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Piste> retrieveAllPistes() {
        return pisteRepository.findAll();
    }

    @Override
    public Piste addPiste(Piste piste) {
        Checks.require(piste.getNumPiste() == null, "Identifiant généré à la création");
        validate(piste);
        piste.setSkiers(new java.util.HashSet<>());
        return pisteRepository.save(piste);
    }

    @Override
    public void removePiste(Long numPiste) {
        Piste piste = retrievePiste(numPiste);
        if (!piste.getSkiers().isEmpty()) throw BusinessException.conflict("Piste affectée à des skieurs : suppression refusée");
        pisteRepository.delete(piste);
    }

    @Override
    @Transactional(readOnly = true)
    public Piste retrievePiste(Long numPiste) {
        Checks.id(numPiste);
        return pisteRepository.findById(numPiste).orElseThrow(() -> BusinessException.missing("Piste"));
    }
    private void validate(Piste piste) {
        Checks.text(piste.getNamePiste(), "Nom de piste");
        Checks.require(piste.getColor() != null && piste.getLength() > 0 && piste.getSlope() >= 0 && piste.getSlope() <= 90,
                "Couleur, longueur positive et pente entre 0 et 90 degrés requises");
    }
    @Override
    public Piste updatePiste(Piste input) {
        validate(input);
        Piste current = retrievePiste(input.getNumPiste());
        current.setNamePiste(input.getNamePiste()); current.setColor(input.getColor());
        current.setLength(input.getLength()); current.setSlope(input.getSlope());
        return pisteRepository.save(current);
    }
    @Override
    @Transactional(readOnly = true)
    public List<Piste> search(Color color) {
        return pisteRepository.findAll().stream().filter(p -> color == null || p.getColor() == color).collect(java.util.stream.Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public java.util.Set<Skier> skiers(Long id) { return retrievePiste(id).getSkiers(); }
}
