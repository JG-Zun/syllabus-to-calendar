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

    @Test
    void readsBrightspaceCalendarName() {
        String ics = "BEGIN:VCALENDAR\r\nX-WR-CALNAME:CSI 2110 - Fall 2026\r\n"
                + "BEGIN:VEVENT\r\nSUMMARY:Assignment 1\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        assertEquals("CSI 2110 - Fall 2026", IcsReader.calendarName(ics));
    }

    @Test
    void retainsDescriptionUrlLocationAndOtherProperties() {
        String ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:lab-1\r\nSUMMARY:Soldering lab submission - Due\r\n"
                + "DESCRIPTION:GNG2101B | F26 | Prof. Lakhani\\nFollow the lab manual\\nB1 - Nov 9\r\n"
                + "URL:https://brightspace.example/assignment/1\r\nLOCATION:Engineering lab\r\n"
                + "COMMENT:Bring safety glasses\r\nDTSTART;VALUE=DATE:20261109\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n";
        var entry = IcsReader.read(ics, ZoneId.of("America/Toronto")).get(0);

        assertEquals("GNG2101B | F26 | Prof. Lakhani\nFollow the lab manual\nB1 - Nov 9", entry.description());
        assertEquals("https://brightspace.example/assignment/1", entry.url());
        assertEquals("Engineering lab", entry.location());
        assertEquals("Bring safety glasses", entry.properties().get("COMMENT").get(0));
    }
}