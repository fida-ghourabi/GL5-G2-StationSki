package com.example.gestionstationskii.services.piste;

import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.controllers.PisteRestController;
import com.example.gestionstationskii.dto.Requests;
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
    @Autowired private PisteRestController controller;
    @Autowired private EntityManager entityManager;

    private Requests.PisteInput pisteInput() {
        Requests.PisteInput input = new Requests.PisteInput();
        input.setNamePiste("Test piste");
        input.setColor(Color.BLUE);
        input.setLength(1200);
        input.setSlope(20);
        return input;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void createPiste() {
        Piste saved = controller.addPiste(pisteInput());
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
        Long id = controller.addPiste(pisteInput()).getNumPiste();
        flushAndClear();
        Piste result = controller.getById(id);
        assertEquals(id, result.getNumPiste());
        assertEquals("Test piste", result.getNamePiste());
        assertTrue(controller.getAllPistes().stream().anyMatch(p -> id.equals(p.getNumPiste())));
    }

    @Test
    void updatePiste() {
        Long id = controller.addPiste(pisteInput()).getNumPiste();
        flushAndClear();
        Requests.PisteInput changes = pisteInput();
        changes.setNumPiste(id);
        changes.setNamePiste("Updated piste");
        changes.setColor(Color.RED);
        changes.setLength(1800);
        changes.setSlope(35);
        assertEquals(id, controller.update(changes).getNumPiste());
        flushAndClear();
        Piste stored = controller.getById(id);
        assertEquals("Updated piste", stored.getNamePiste());
        assertEquals(Color.RED, stored.getColor());
        assertEquals(1800, stored.getLength());
        assertEquals(35, stored.getSlope());
    }

    @Test
    void deletePiste() {
        Long id = controller.addPiste(pisteInput()).getNumPiste();
        flushAndClear();
        controller.deleteById(id);
        flushAndClear();
        assertNull(entityManager.find(Piste.class, id));
        BusinessException error = assertThrows(BusinessException.class, () -> controller.getById(id));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
