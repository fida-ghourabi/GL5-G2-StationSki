package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.services.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.DateTimeException;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionHandlerTest {

    private static final String URI = "/skier/get/abc";

    private final ApiExceptionHandler handler = new ApiExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", URI);

    private static void assertBody(ResponseEntity<ApiExceptionHandler.ErrorResponse> response, HttpStatus status) {
        assertEquals(status, response.getStatusCode());
        ApiExceptionHandler.ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(status.value(), body.getStatus());
        assertEquals(URI, body.getPath());
        assertNotNull(body.getTimestamp());
        assertNotNull(body.getMessage());
    }

    @Test
    void businessExceptionKeepsItsStatusAndMessage() {
        ResponseEntity<ApiExceptionHandler.ErrorResponse> notFound =
                handler.business(BusinessException.missing("Skieur"), request);
        assertBody(notFound, HttpStatus.NOT_FOUND);
        assertEquals("Skieur introuvable", notFound.getBody().getMessage());

        ResponseEntity<ApiExceptionHandler.ErrorResponse> conflict =
                handler.business(BusinessException.conflict("Doublon"), request);
        assertBody(conflict, HttpStatus.CONFLICT);
        assertEquals("Doublon", conflict.getBody().getMessage());

        assertBody(handler.business(new BusinessException(HttpStatus.BAD_REQUEST, "Invalide"), request), HttpStatus.BAD_REQUEST);
    }

    @Test
    void malformedRequestsAreMappedToBadRequestWithGenericMessage() {
        Exception[] errors = {
                new HttpMessageNotReadableException("JSON illisible", new MockHttpInputMessage(new byte[0])),
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null),
                new MissingServletRequestParameterException("from", "LocalDateTime"),
                new IllegalArgumentException("argument"),
                new DateTimeException("date")
        };
        for (Exception error : errors) {
            ResponseEntity<ApiExceptionHandler.ErrorResponse> response = handler.invalid(error, request);
            assertBody(response, HttpStatus.BAD_REQUEST);
            assertTrue(response.getBody().getMessage().startsWith("Requête invalide"));
        }
    }

    @Test
    void dataIntegrityAndConcurrencyFailuresAreMappedToConflict() {
        Exception[] errors = {
                new DataIntegrityViolationException("contrainte"),
                new ConcurrencyFailureException("verrou")
        };
        for (Exception error : errors) {
            ResponseEntity<ApiExceptionHandler.ErrorResponse> response = handler.conflict(error, request);
            assertBody(response, HttpStatus.CONFLICT);
            assertFalse(response.getBody().getMessage().contains("contrainte"), "internal details must not leak");
        }
    }
}
