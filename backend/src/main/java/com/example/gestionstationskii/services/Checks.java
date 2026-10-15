package com.example.gestionstationskii.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;

public final class Checks {
    private Checks() { }
    public static void require(boolean condition, String message) {
        if (!condition) throw new BusinessException(HttpStatus.BAD_REQUEST, message);
    }
    public static void id(Long id) { require(id != null && id > 0, "Identifiant positif requis"); }
    public static void text(String value, String field) {
        require(value != null && !value.trim().isEmpty() && value.length() <= 255, field + " requis (255 caractères maximum)");
    }
    public static void money(BigDecimal value) {
        require(value != null && value.signum() >= 0 && value.scale() <= 2 && value.compareTo(new BigDecimal("9999999999.99")) <= 0,
                "Montant positif ou nul, avec au plus deux décimales et 12 chiffres requis");
    }
    public static void price(Float price) {
        require(price != null && !price.isNaN() && !price.isInfinite() && price >= 0, "Prix positif ou nul requis");
    }
    public static void period(LocalDate from, LocalDate to) {
        require(from != null && to != null && from.isBefore(to), "La fin doit être postérieure au début");
    }
}
