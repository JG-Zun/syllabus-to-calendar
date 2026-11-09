package com.JaskaranDev.syllabus_to_calendar.privacy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedactorTest {

    private final Redactor redactor = new Redactor();

    @Test
    void redactsEmails() {
        Redactor.Result r = redactor.redact(
                "Questions? Email prof.smith@university.ca before the Midterm on Oct 24.");
        assertTrue(r.text().contains("[EMAIL]"));
        assertFalse(r.text().contains("@"));
        assertTrue(r.text().contains("Midterm on Oct 24"));
        assertEquals(1, r.emailsRedacted());
    }

    @Test
    void redactsPhoneNumbers() {
        String[] numbers = {"613-555-0123", "(613) 555 0123", "+1 613 555 0123", "613.555.0123"};
        for (String n : numbers) {
            Redactor.Result r = redactor.redact("Call " + n + " about the quiz.");
            assertFalse(r.text().contains("555"), n);
            assertEquals(1, r.phonesRedacted(), n);
        }
    }

    @Test
    void dropsOfficeHoursLines() {
        Redactor.Result r = redactor.redact("Office Hours: Tuesdays 2-4pm\nMidterm exam: October 24");
        assertFalse(r.text().toLowerCase().contains("office hours"));
        assertTrue(r.text().contains("Midterm exam: October 24"));
        assertEquals(1, r.linesDropped());
    }

    @Test
    void dropsContactLabelLines() {
        Redactor.Result r = redactor.redact(
                "Instructor: Dr. Jane Smith\nTA: Alex Lee\nFinal exam: December 12");
        assertFalse(r.text().contains("Jane Smith"));
        assertFalse(r.text().contains("Alex Lee"));
        assertTrue(r.text().contains("Final exam: December 12"));
        assertEquals(2, r.linesDropped());
    }

    @Test
    void leavesDeadlineLinesUntouched() {
        Redactor.Result r = redactor.redact(
                "Week 5 - Oct 24: Midterm exam (worth 30%)\nAssignment 2 due Nov 3");
        assertEquals(0, r.linesDropped());
        assertTrue(r.text().contains("Midterm exam (worth 30%)"));
        assertTrue(r.text().contains("Assignment 2 due Nov 3"));
    }

    @Test
    void doesNotMistakeDatesForPhoneNumbers() {
        Redactor.Result r = redactor.redact("Due 2026-11-20 and 11.20.2026 and 20261120");
        assertEquals(0, r.phonesRedacted());
    }
}
