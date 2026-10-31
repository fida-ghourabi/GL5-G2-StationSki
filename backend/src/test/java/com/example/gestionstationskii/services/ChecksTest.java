package com.example.gestionstationskii.services;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static com.example.gestionstationskii.services.BusinessAssertions.assertBadRequest;
import static org.junit.jupiter.api.Assertions.*;

class ChecksTest {

    @Test
    void requireAcceptsTrueConditionAndRejectsFalseWithMessage() {
        assertDoesNotThrow(() -> Checks.require(true, "ignored"));

        BusinessException ex = assertBadRequest(() -> Checks.require(false, "Message métier"));
        assertEquals("Message métier", ex.getMessage());
    }

    @Test
    void idAcceptsOnlyPositiveValues() {
        assertDoesNotThrow(() -> Checks.id(1L));
        assertDoesNotThrow(() -> Checks.id(Long.MAX_VALUE));
        assertBadRequest(() -> Checks.id(null));
        assertBadRequest(() -> Checks.id(0L));
        assertBadRequest(() -> Checks.id(-1L));
    }

    @Test
    void textAcceptsNonBlankValuesUpTo255Characters() {
        assertDoesNotThrow(() -> Checks.text("Alpine", "Nom"));
        assertDoesNotThrow(() -> Checks.text(repeat('a', 255), "Nom"));
    }

    @Test
    void textRejectsNullBlankAndTooLongValuesWithFieldNameInMessage() {
        BusinessException ex = assertBadRequest(() -> Checks.text(null, "Prénom"));
        assertTrue(ex.getMessage().startsWith("Prénom"));
        assertBadRequest(() -> Checks.text("", "Nom"));
        assertBadRequest(() -> Checks.text("   ", "Nom"));
        assertBadRequest(() -> Checks.text(repeat('a', 256), "Nom"));
    }

    @Test
    void moneyAcceptsZeroAndTwoDecimalsUpToTheMaximum() {
        assertDoesNotThrow(() -> Checks.money(BigDecimal.ZERO));
        assertDoesNotThrow(() -> Checks.money(new BigDecimal("10")));
        assertDoesNotThrow(() -> Checks.money(new BigDecimal("10.50")));
        assertDoesNotThrow(() -> Checks.money(new BigDecimal("9999999999.99")));
    }

    @Test
    void moneyRejectsNullNegativeTooPreciseAndTooLargeAmounts() {
        assertBadRequest(() -> Checks.money(null));
        assertBadRequest(() -> Checks.money(new BigDecimal("-0.01")));
        assertBadRequest(() -> Checks.money(new BigDecimal("1.001")));
        assertBadRequest(() -> Checks.money(new BigDecimal("10000000000.00")));
    }

    @Test
    void priceAcceptsZeroAndPositiveValues() {
        assertDoesNotThrow(() -> Checks.price(0f));
        assertDoesNotThrow(() -> Checks.price(99.9f));
    }

    @Test
    void priceRejectsNullNegativeNaNAndInfinity() {
        assertBadRequest(() -> Checks.price(null));
        assertBadRequest(() -> Checks.price(-1f));
        assertBadRequest(() -> Checks.price(Float.NaN));
        assertBadRequest(() -> Checks.price(Float.POSITIVE_INFINITY));
        assertBadRequest(() -> Checks.price(Float.NEGATIVE_INFINITY));
    }

    @Test
    void utilityClassCannotBeInstantiatedFromOutside() throws Exception {
        Constructor<Checks> constructor = Checks.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
        assertEquals(HttpStatus.BAD_REQUEST, assertBadRequest(() -> Checks.id(0L)).getStatus());
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times; i++) sb.append(c);
        return sb.toString();
    }
}
