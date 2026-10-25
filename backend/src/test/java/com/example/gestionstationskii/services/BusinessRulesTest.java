package com.example.gestionstationskii.services;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Isolated business tests: no Spring context, database or banking provider. */
class BusinessRulesTest {
    @Test
    void adjacentLessonsDoNotOverlap() {
        LocalDateTime start = LocalDate.of(2027, 1, 1).atTime(9, 0);
        assertFalse(TimeRanges.overlaps(start, start.plusHours(1), start.plusHours(1), start.plusHours(2)));
        assertTrue(TimeRanges.overlaps(start, start.plusHours(1), start.plusMinutes(30), start.plusHours(2)));
    }

}
