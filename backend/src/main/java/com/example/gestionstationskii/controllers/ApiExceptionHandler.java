package com.example.gestionstationskii.controllers;

import com.example.gestionstationskii.services.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestControllerAdvice
public class ApiExceptionHandler {
    @Getter @AllArgsConstructor
    public static class ErrorResponse {
        private final LocalDateTime timestamp;
        private final int status;
        private final String message;
        private final String path;
    }
    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ErrorResponse(LocalDateTime.now(ZoneId.systemDefault()), status.value(), message, request.getRequestURI()));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException ex, HttpServletRequest request) {
        return error(ex.getStatus(), ex.getMessage(), request);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, IllegalArgumentException.class, java.time.DateTimeException.class})
    public ResponseEntity<ErrorResponse> invalid(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Requête invalide : vérifier les champs requis, les types et les dates ISO", request);
    }
    @ExceptionHandler({DataIntegrityViolationException.class, ConcurrencyFailureException.class})
    public ResponseEntity<ErrorResponse> conflict(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "Conflit de données ou opération concurrente ; vérifier les associations et réessayer", request);
    }
}
