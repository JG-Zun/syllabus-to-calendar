package com.JaskaranDev.syllabus_to_calendar.extract;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TermDetectorTest {

    @Test
    void detectsSeasonAndYear() {
        var term = TermDetector.detect("COURSE OUTLINE\nCLA 2323 - Fall 2026").orElseThrow();
        assertEquals(LocalDate.of(2026, 9, 1), term.start());
    }

    @Test
    void detectsWinterTerm() {
        var term = TermDetector.detect("Winter 2027 Syllabus").orElseThrow();
        assertEquals(LocalDate.of(2027, 1, 1), term.start());
    }

    @Test
    void detectsDateRange() {
        var term = TermDetector.detect("Term: September 6 - December 6, 2026").orElseThrow();
        assertEquals(LocalDate.of(2026, 9, 6), term.start());
    }

    @Test
    void returnsEmptyWhenNothingFound() {
        assertTrue(TermDetector.detect("Welcome to the course").isEmpty());
    }
}
