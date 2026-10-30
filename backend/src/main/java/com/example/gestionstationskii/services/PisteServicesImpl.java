package com.example.gestionstationskii.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gestionstationskii.entities.*;
import com.example.gestionstationskii.repositories.*;

import java.util.List;
@lombok.extern.slf4j.Slf4j
@RequiredArgsConstructor
@Service
@Transactional
public class PisteServicesImpl implements IPisteServices {

    private final IPisteRepository pisteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Piste> retrieveAllPistes() {
        return logged("list", null, () -> {
            List<Piste> pistes = pisteRepository.findAll();
            log.debug("Piste list returned count={}", pistes.size());
            return pistes;
        });
    }

    @Override
    public Piste addPiste(Piste piste) {
        return logged("create", null, () -> {
        Checks.require(piste.getNumPiste() == null, "Identifiant généré à la création");
        validate(piste);
        piste.setSkiers(new java.util.HashSet<>());
        Piste saved = pisteRepository.save(piste);
        log.info("Piste created id={}", saved.getNumPiste());
        return saved;
        });
    }

    @Override
    public void removePiste(Long numPiste) {
        logged("delete", numPiste, () -> {
        Piste piste = retrievePiste(numPiste);
        if (!piste.getSkiers().isEmpty()) throw BusinessException.conflict("Piste affectée à des skieurs : suppression refusée");
        pisteRepository.delete(piste);
        log.info("Piste deletion submitted id={}", numPiste);
        return null;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Piste retrievePiste(Long numPiste) {
        return logged("read", numPiste, () -> {
            Checks.id(numPiste);
            return pisteRepository.findById(numPiste).orElseThrow(() -> BusinessException.missing("Piste"));
        });
    }
    // Calls run inside the service transaction; completion here does not imply commit.
    private <T> T logged(String operation, Long id, java.util.function.Supplier<T> action) {
        log.debug("Piste operation={} requested id={}", operation, id);
        try {
            T result = action.get();
            log.debug("Piste operation={} completed id={}", operation, id);
            return result;
        } catch (RuntimeException ex) {
            log.error("Piste operation={} failed id={}", operation, id, ex);
            throw ex;
        }
    }

    private void validate(Piste piste) {
        Checks.text(piste.getNamePiste(), "Nom de piste");
        Checks.require(piste.getColor() != null && piste.getLength() > 0 && piste.getSlope() >= 0 && piste.getSlope() <= 90,
                "Couleur, longueur positive et pente entre 0 et 90 degrés requises");
    }
    @Override
    public Piste updatePiste(Piste input) {
        return logged("update", input == null ? null : input.getNumPiste(), () -> {
        validate(input);
        Piste current = retrievePiste(input.getNumPiste());
        current.setNamePiste(input.getNamePiste()); current.setColor(input.getColor());
        current.setLength(input.getLength()); current.setSlope(input.getSlope());
        Piste saved = pisteRepository.save(current);
        log.info("Piste updated id={} color={} length={} slope={}",
                saved.getNumPiste(), saved.getColor(), saved.getLength(), saved.getSlope());
        return saved;
        });
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
