package com.example.gestionstationskii.services.skier;

import com.example.gestionstationskii.services.BusinessException;
import com.example.gestionstationskii.controllers.SkierRestController;
import com.example.gestionstationskii.dto.Requests;
import com.example.gestionstationskii.entities.Skier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/** Real service/repository integration; no Mockito or application database. */
@SpringBootTest
@ActiveProfiles("crud-test")
@Transactional
class SkierCrudIntegrationTest {
    @Autowired private SkierRestController controller;
    @Autowired private EntityManager entityManager;

    private Requests.SkierInput skierInput() {
        Requests.SkierInput input = new Requests.SkierInput();
        input.setFirstName("Test");
        input.setLastName("Skier");
        input.setCity("Test city");
        input.setDateOfBirth(LocalDate.of(2000, 1, 15));
        return input;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void createSkier() {
        Long id = controller.addSkier(skierInput()).getNumSkier();
        assertNotNull(id);
        flushAndClear();
        Skier stored = entityManager.find(Skier.class, id);
        assertNotNull(stored);
        assertEquals("Test", stored.getFirstName());
        assertEquals("Skier", stored.getLastName());
        assertEquals("Test city", stored.getCity());
        assertEquals(LocalDate.of(2000, 1, 15), stored.getDateOfBirth());
    }

    @Test
    void readSkier() {
        Long id = controller.addSkier(skierInput()).getNumSkier();
        flushAndClear();
        Skier result = controller.getById(id);
        assertEquals(id, result.getNumSkier());
        assertEquals("Test", result.getFirstName());
        assertTrue(controller.getAllSkiers().stream().anyMatch(s -> id.equals(s.getNumSkier())));
    }

    @Test
    void updateSkier() {
        Long id = controller.addSkier(skierInput()).getNumSkier();
        flushAndClear();
        Requests.SkierInput changes = skierInput();
        changes.setNumSkier(id);
        changes.setFirstName("Updated");
        changes.setLastName("Tester");
        changes.setCity("Another city");
        changes.setDateOfBirth(LocalDate.of(1998, 5, 20));
        assertEquals(id, controller.update(changes).getNumSkier());
        flushAndClear();
        Skier stored = controller.getById(id);
        assertEquals("Updated", stored.getFirstName());
        assertEquals("Tester", stored.getLastName());
        assertEquals("Another city", stored.getCity());
        assertEquals(LocalDate.of(1998, 5, 20), stored.getDateOfBirth());
    }

    @Test
    void deleteSkier() {
        Long id = controller.addSkier(skierInput()).getNumSkier();
        flushAndClear();
        controller.deleteById(id);
        flushAndClear();
        assertNull(entityManager.find(Skier.class, id));
        BusinessException error = assertThrows(BusinessException.class, () -> controller.getById(id));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
