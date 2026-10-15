package com.example.gestionstationskii.services;

import java.time.LocalDateTime;

/** Half-open intervals [start, end); adjacent lessons do not overlap. */
final class TimeRanges {
    private TimeRanges() { }

    static boolean overlaps(LocalDateTime start, LocalDateTime end, LocalDateTime otherStart, LocalDateTime otherEnd) {
        return start.isBefore(otherEnd) && otherStart.isBefore(end);
    }
}
