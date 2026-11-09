package com.JaskaranDev.syllabus_to_calendar.export;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IcsExporterTest {

    private final IcsExporter exporter = new IcsExporter();

    private List<CalendarEvent> sample() {
        return List.of(
                new CalendarEvent("Midterm Exam", LocalDate.of(2026, 11, 20), EventType.EXAM),
                new CalendarEvent("Assignment 2 due", LocalDate.of(2026, 11, 27), EventType.ASSIGNMENT),
                new CalendarEvent("Final Project, Part 1", LocalDate.of(2026, 12, 4), EventType.PROJECT));
    }

    @Test
    void producesValidStructure() {
        String ics = exporter.export(sample());
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertEquals(3, ics.split("BEGIN:VEVENT", -1).length - 1);
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261120"));
        assertTrue(ics.contains("DTEND;VALUE=DATE:20261121"));
    }

    @Test
    void escapesCommasInTitles() {
        String ics = exporter.export(sample());
        assertTrue(ics.contains("SUMMARY:Final Project\\, Part 1"));
    }

    @Test
    void writesSampleFileForManualImport() throws IOException {
        Path out = Path.of("target", "sample.ics");
        exporter.exportToFile(sample(), out);
        assertTrue(Files.exists(out));
        System.out.println("Wrote " + out.toAbsolutePath());
    }
}