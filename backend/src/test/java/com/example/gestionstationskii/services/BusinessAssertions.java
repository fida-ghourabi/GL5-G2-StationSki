package com.example.gestionstationskii.services;

import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Shared helper: asserts that a call fails with a BusinessException carrying the expected HTTP status. */
final class BusinessAssertions {
    private BusinessAssertions() { }

    static BusinessException assertBusiness(HttpStatus expected, Executable call) {
        BusinessException ex = assertThrows(BusinessException.class, call);
        assertEquals(expected, ex.getStatus());
        return ex;
    }

    static BusinessException assertBadRequest(Executable call) {
        return assertBusiness(HttpStatus.BAD_REQUEST, call);
    }

    static BusinessException assertNotFound(Executable call) {
        return assertBusiness(HttpStatus.NOT_FOUND, call);
    }

    static BusinessException assertConflict(Executable call) {
        return assertBusiness(HttpStatus.CONFLICT, call);
    }
}
