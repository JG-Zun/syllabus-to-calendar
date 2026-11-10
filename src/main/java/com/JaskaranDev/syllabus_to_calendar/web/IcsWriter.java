package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class IcsWriter {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public static String write(List<CalendarEvent> events) {
        StringBuilder sb = new StringBuilder();
        String stamp = STAMP.format(ZonedDateTime.now(ZoneOffset.UTC));
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//Syllabus to Calendar//EN");
        line(sb, "CALSCALE:GREGORIAN");
        for (CalendarEvent e : events) {
            // Same event always gets the same UID, so re-importing updates instead of duplicating
            String key = e.date() + "|" + e.type() + "|" + e.title();
            String uid = UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
            line(sb, "BEGIN:VEVENT");
            line(sb, "UID:" + uid + "@syllabus-to-calendar");
            line(sb, "DTSTAMP:" + stamp);
            line(sb, "DTSTART;VALUE=DATE:" + DAY.format(e.date()));
            line(sb, "DTEND;VALUE=DATE:" + DAY.format(e.date().plusDays(1)));
            line(sb, "SUMMARY:" + escape(e.title()));
            line(sb, "CATEGORIES:" + e.type());
            line(sb, "END:VEVENT");
        }
        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }

    // The calendar format wants lines folded at 75 characters, with CRLF line endings
    private static void line(StringBuilder sb, String text) {
        while (text.length() > 73) {
            sb.append(text, 0, 73).append("\r\n ");
            text = text.substring(73);
        }
        sb.append(text).append("\r\n");
    }
}