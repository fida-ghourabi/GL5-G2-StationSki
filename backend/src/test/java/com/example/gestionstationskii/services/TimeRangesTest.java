package com.example.gestionstationskii.services;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimeRangesTest {

    private static LocalDateTime at(int hour) {
        return LocalDateTime.of(2030, 1, 15, hour, 0);
    }

    @Test
    void overlappingIntervalsAreDetected() {
        assertTrue(TimeRanges.overlaps(at(10), at(12), at(11), at(13)));
        assertTrue(TimeRanges.overlaps(at(11), at(13), at(10), at(12)));
    }

    @Test
    void containedAndIdenticalIntervalsOverlap() {
        assertTrue(TimeRanges.overlaps(at(9), at(17), at(10), at(11)));
        assertTrue(TimeRanges.overlaps(at(10), at(11), at(9), at(17)));
        assertTrue(TimeRanges.overlaps(at(10), at(11), at(10), at(11)));
    }

    @Test
    void adjacentIntervalsDoNotOverlap() {
        assertFalse(TimeRanges.overlaps(at(10), at(11), at(11), at(12)));
        assertFalse(TimeRanges.overlaps(at(11), at(12), at(10), at(11)));
    }

    @Test
    void disjointIntervalsDoNotOverlap() {
        assertFalse(TimeRanges.overlaps(at(8), at(9), at(14), at(15)));
        assertFalse(TimeRanges.overlaps(at(14), at(15), at(8), at(9)));
    }

    @Test
    void utilityClassHasPrivateConstructor() throws Exception {
        Constructor<TimeRanges> constructor = TimeRanges.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }
}
