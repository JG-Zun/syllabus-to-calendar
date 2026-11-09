package com.JaskaranDev.syllabus_to_calendar.extract;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleBasedExtractorTest {

    private static final LocalDate TERM_START = LocalDate.of(2026, 9, 8);
    private final RuleBasedExtractor extractor = new RuleBasedExtractor();

    private List<CalendarEvent> run(String text) {
        return extractor.extract(text, TERM_START);
    }

    private static CalendarEvent event(String title, int month, int day, EventType type) {
        return new CalendarEvent(title, LocalDate.of(2026, month, day), type);
    }

    @Test
    void parsesMonthFirstDate() {
        assertEquals(List.of(event("Midterm Exam", 10, 24, EventType.EXAM)),
                run("Midterm Exam - Oct 24"));
    }

    @Test
    void parsesDayFirstDate() {
        assertEquals(List.of(event("Final exam", 10, 24, EventType.EXAM)),
                run("24 October: Final exam"));
    }

    @Test
    void parsesWeekdayAndExplicitYear() {
        assertEquals(List.of(event("Quiz 2", 11, 3, EventType.QUIZ)),
                run("Tuesday, November 3, 2026 - Quiz 2"));
    }

    @Test
    void parsesIsoDate() {
        assertEquals(List.of(event("Assignment 1", 10, 3, EventType.ASSIGNMENT)),
                run("Assignment 1 due 2026-10-03"));
    }

    @Test
    void prefersTheDateAfterDue() {
        List<CalendarEvent> result = run("Assignment 2 (released Sep 19) due Oct 3");
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 10, 3), result.get(0).date());
        assertEquals(EventType.ASSIGNMENT, result.get(0).type());
    }

    @Test
    void rollsIntoNextYearForJanuaryDates() {
        assertEquals(List.of(new CalendarEvent("Quiz 5", LocalDate.of(2027, 1, 15), EventType.QUIZ)),
                run("Quiz 5 - Jan 15"));
    }

    @Test
    void ignoresDatesWithoutKeywords() {
        assertTrue(run("Oct 24: Lecture on binary trees").isEmpty());
    }

    @Test
    void ignoresNoClassReviewAndReleaseLines() {
        String text = "Oct 24: No class (midterm review)\n"
                + "Midterm review: Oct 20\n"
                + "Assignment 1 released Sep 19";
        assertTrue(run(text).isEmpty());
    }

    @Test
    void splitsSemicolonSeparatedEvents() {
        assertEquals(List.of(event("Quiz 1", 10, 3, EventType.QUIZ),
                        event("Quiz 2", 10, 17, EventType.QUIZ)),
                run("Quiz 1: Oct 3; Quiz 2: Oct 17"));
    }

    @Test
    void skipsInvalidDates() {
        assertTrue(run("Midterm: Feb 30").isEmpty());
    }

    @Test
    void monthFirstWinsWhenReadingsOverlap() {
        List<CalendarEvent> result = run("Quiz 3 Oct 5");
        assertEquals(1, result.size());
        assertEquals(LocalDate.of(2026, 10, 5), result.get(0).date());
    }

    @Test
    void removesDuplicates() {
        assertEquals(1, run("Midterm Exam - Oct 24\nMidterm Exam - Oct 24").size());
    }

    @Test
    void classifiesByFirstSpecificKeyword() {
        assertEquals(EventType.PROJECT, run("Final project due Dec 4").get(0).type());
        assertEquals(EventType.QUIZ, run("Due: Quiz 3 on Nov 12").get(0).type());
        assertEquals(EventType.OTHER, run("Submit reflection by Nov 20").get(0).type());
    }
}
