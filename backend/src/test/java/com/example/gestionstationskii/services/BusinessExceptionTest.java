package com.example.gestionstationskii.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {

    @Test
    void constructorKeepsStatusAndMessage() {
        BusinessException ex = new BusinessException(HttpStatus.FORBIDDEN, "Interdit");

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Interdit", ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void missingBuildsNotFoundExceptionNamingTheResource() {
        BusinessException ex = BusinessException.missing("Skieur");

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Skieur introuvable", ex.getMessage());
    }

    @Test
    void conflictBuildsConflictException() {
        BusinessException ex = BusinessException.conflict("Déjà affecté");

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Déjà affecté", ex.getMessage());
    }
}
