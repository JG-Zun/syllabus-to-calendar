package com.JaskaranDev.syllabus_to_calendar.web;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IcsReaderTest {
    @Test
    void convertsUtcDeadlineToLocalDate() {
        String ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:Assignment 1 - Due\r\n"
                + "DTSTART:20261004T035900Z\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        var entries = IcsReader.read(ics, ZoneId.of("America/Toronto"));
        assertEquals(1, entries.size());
        assertEquals(LocalDate.of(2026, 10, 3), entries.get(0).date()); // 11:59 PM the night before
    }
}