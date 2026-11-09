package com.JaskaranDev.syllabus_to_calendar.export;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class IcsExporter {

    private static final String CRLF = "\r\n";
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public String export(List<CalendarEvent> events) {
        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//SyllabusToCalendar//EN");
        line(sb, "CALSCALE:GREGORIAN");

        String stamp = STAMP.format(Instant.now());
        for (CalendarEvent e : events) {
            line(sb, "BEGIN:VEVENT");
            line(sb, "UID:" + uidFor(e) + "@syllabustocalendar");
            line(sb, "DTSTAMP:" + stamp);
            line(sb, "DTSTART;VALUE=DATE:" + DATE.format(e.date()));
            line(sb, "DTEND;VALUE=DATE:" + DATE.format(e.date().plusDays(1)));
            line(sb, "SUMMARY:" + escape(e.title()));
            line(sb, "CATEGORIES:" + e.type().name());
            line(sb, "END:VEVENT");
        }

        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    public void exportToFile(List<CalendarEvent> events, Path path) throws IOException {
        Files.writeString(path, export(events), StandardCharsets.UTF_8);
    }

    private static void line(StringBuilder sb, String content) {
        sb.append(content).append(CRLF);
    }

    private static String uidFor(CalendarEvent e) {
        String key = e.title() + "|" + e.date();
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}
