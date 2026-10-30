package com.example.gestionstationskii.services;

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
    @Autowired private ISkierServices service;
    @Autowired private EntityManager entityManager;

    private Skier skier() {
        Skier skier = new Skier();
        skier.setFirstName("Test");
        skier.setLastName("Skier");
        skier.setCity("Test city");
        skier.setDateOfBirth(LocalDate.of(2000, 1, 15));
        return skier;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void createSkier() {
        Long id = service.addSkier(skier()).getNumSkier();
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
        Long id = service.addSkier(skier()).getNumSkier();
        flushAndClear();
        Skier result = service.retrieveSkier(id);
        assertEquals(id, result.getNumSkier());
        assertEquals("Test", result.getFirstName());
        assertTrue(service.retrieveAllSkiers().stream().anyMatch(s -> id.equals(s.getNumSkier())));
    }

    @Test
    void updateSkier() {
        Long id = service.addSkier(skier()).getNumSkier();
        flushAndClear();
        Skier changes = skier();
        changes.setNumSkier(id);
        changes.setFirstName("Updated");
        changes.setLastName("Tester");
        changes.setCity("Another city");
        changes.setDateOfBirth(LocalDate.of(1998, 5, 20));
        assertEquals(id, service.updateSkier(changes).getNumSkier());
        flushAndClear();
        Skier stored = service.retrieveSkier(id);
        assertEquals("Updated", stored.getFirstName());
        assertEquals("Tester", stored.getLastName());
        assertEquals("Another city", stored.getCity());
        assertEquals(LocalDate.of(1998, 5, 20), stored.getDateOfBirth());
    }

    @Test
    void deleteSkier() {
        Long id = service.addSkier(skier()).getNumSkier();
        flushAndClear();
        service.removeSkier(id);
        flushAndClear();
        assertNull(entityManager.find(Skier.class, id));
        BusinessException error = assertThrows(BusinessException.class, () -> service.retrieveSkier(id));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}
