package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IcsWriterTest {

    @Test
    void writesAllDayEventsAndEscapesText() {
        String ics = IcsWriter.write(List.of(
                new CalendarEvent("Quiz 1, part A", LocalDate.of(2026, 10, 3), EventType.QUIZ)));
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261003"));
        assertTrue(ics.contains("DTEND;VALUE=DATE:20261004"));
        assertTrue(ics.contains("SUMMARY:Quiz 1\\, part A"));
        assertTrue(ics.contains("\r\n"));
    }
}
