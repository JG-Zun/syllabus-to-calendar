package com.JaskaranDev.syllabus_to_calendar.extract;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseDetectorTest {

    private final CourseDetector detector = new CourseDetector();

    @Test
    void findsCodeWithSpace() {
        assertEquals(Optional.of("CSI 2110"),
                detector.detect("CSI 2110 Data Structures - Fall 2026\nWeek 1"));
    }

    @Test
    void normalizesCodeWithoutSpace() {
        assertEquals(Optional.of("CSI 2110"), detector.detect("Welcome\nCSI2110: Syllabus"));
    }

    @Test
    void handlesHyphenAndLetterSuffix() {
        assertEquals(Optional.of("MAT 1320"), detector.detect("MAT-1320 Calculus"));
        assertEquals(Optional.of("SEG 2105A"), detector.detect("SEG 2105A Software"));
    }

    @Test
    void doesNotMistakeTermAndYearForACourse() {
        assertEquals(Optional.of("CSI 2110"), detector.detect("FALL 2026 SYLLABUS\nCSI 2110"));
        assertTrue(detector.detect("Syllabus\nFall 2026\nUniversity of Ottawa").isEmpty());
    }

    @Test
    void onlyScansTheTopOfTheDocument() {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            text.append("filler line ").append(i).append("\n");
        }
        text.append("CSI 2110");
        assertTrue(detector.detect(text.toString()).isEmpty());
    }
}