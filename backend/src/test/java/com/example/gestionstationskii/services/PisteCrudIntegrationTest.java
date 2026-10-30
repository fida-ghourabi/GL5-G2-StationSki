package com.example.gestionstationskii.services;

import com.example.gestionstationskii.entities.Color;
import com.example.gestionstationskii.entities.Piste;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

/** Real service/repository integration; each test rolls back its own data. */
@SpringBootTest
@ActiveProfiles("crud-test")
@Transactional
class PisteCrudIntegrationTest {
    @Autowired private IPisteServices service;
    @Autowired private EntityManager entityManager;

    private Piste piste() {
        Piste piste = new Piste();
        piste.setNamePiste("Test piste");
        piste.setColor(Color.BLUE);
        piste.setLength(1200);
        piste.setSlope(20);
        return piste;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void createPiste() {
        Piste saved = service.addPiste(piste());
        Long id = saved.getNumPiste();
        assertNotNull(id);
        flushAndClear();
        Piste stored = entityManager.find(Piste.class, id);
        assertNotNull(stored);
        assertEquals("Test piste", stored.getNamePiste());
        assertEquals(Color.BLUE, stored.getColor());
        assertEquals(1200, stored.getLength());
        assertEquals(20, stored.getSlope());
    }

    @Test
    void readPiste() {
        Long id = service.addPiste(piste()).getNumPiste();
        flushAndClear();
        Piste result = service.retrievePiste(id);
        assertEquals(id, result.getNumPiste());
        assertEquals("Test piste", result.getNamePiste());
        assertTrue(service.retrieveAllPistes().stream().anyMatch(p -> id.equals(p.getNumPiste())));
    }

    @Test
    void updatePiste() {
        Long id = service.addPiste(piste()).getNumPiste();
        flushAndClear();
        Piste changes = piste();
        changes.setNumPiste(id);
        changes.setNamePiste("Updated piste");
        changes.setColor(Color.RED);
        changes.setLength(1800);
        changes.setSlope(35);
        assertEquals(id, service.updatePiste(changes).getNumPiste());
        flushAndClear();
        Piste stored = service.retrievePiste(id);
        assertEquals("Updated piste", stored.getNamePiste());
        assertEquals(Color.RED, stored.getColor());
        assertEquals(1800, stored.getLength());
        assertEquals(35, stored.getSlope());
    }

    @Test
    void deletePiste() {
        Long id = service.addPiste(piste()).getNumPiste();
        flushAndClear();
        service.removePiste(id);
        flushAndClear();
        assertNull(entityManager.find(Piste.class, id));
        BusinessException error = assertThrows(BusinessException.class, () -> service.retrievePiste(id));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
