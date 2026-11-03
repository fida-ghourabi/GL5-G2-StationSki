package com.example.gestionstationskii.services;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChecksTest {
    @Test
    void validatesIdentifiersAndTextBoundaries() {
        assertDoesNotThrow(() -> Checks.id(1L));
        assertThrows(BusinessException.class, () -> Checks.id(null));
        assertThrows(BusinessException.class, () -> Checks.id(0L));
        assertThrows(BusinessException.class, () -> Checks.id(-1L));

        assertDoesNotThrow(() -> Checks.text("valid", "Nom"));
        String missingText = null;
        assertThrows(BusinessException.class, () -> Checks.text(missingText, "Nom"));
        String blankText = "  ";
        assertThrows(BusinessException.class, () -> Checks.text(blankText, "Nom"));
        String tooLongText = "x".repeat(256);
        assertThrows(BusinessException.class, () -> Checks.text(tooLongText, "Nom"));
    }

    @Test
    void validatesMoneyPrecisionAndLimit() {
        assertDoesNotThrow(() -> Checks.money(new BigDecimal("0.00")));
        assertDoesNotThrow(() -> Checks.money(new BigDecimal("9999999999.99")));
        BigDecimal missingMoney = null;
        assertThrows(BusinessException.class, () -> Checks.money(missingMoney));
        BigDecimal negativeMoney = new BigDecimal("-0.01");
        assertThrows(BusinessException.class, () -> Checks.money(negativeMoney));
        BigDecimal overPreciseMoney = new BigDecimal("1.001");
        assertThrows(BusinessException.class, () -> Checks.money(overPreciseMoney));
        BigDecimal excessiveMoney = new BigDecimal("10000000000.00");
        assertThrows(BusinessException.class, () -> Checks.money(excessiveMoney));
    }

    @Test
    void validatesFiniteNonNegativePrices() {
        assertDoesNotThrow(() -> Checks.price(0F));
        assertDoesNotThrow(() -> Checks.price(12.5F));
        assertThrows(BusinessException.class, () -> Checks.price(null));
        assertThrows(BusinessException.class, () -> Checks.price(Float.NaN));
        assertThrows(BusinessException.class, () -> Checks.price(Float.POSITIVE_INFINITY));
        assertThrows(BusinessException.class, () -> Checks.price(-0.01F));
    }
}
