package com.JaskaranDev.syllabus_to_calendar.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class IcsReader {


    private static final DateTimeFormatter UTC_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    public record Entry(String uid, String summary, String description, LocalDate date) {}

    public static List<Entry> read(String ics, ZoneId zone) {
        String text = ics.replace("\r\n", "\n").replaceAll("\n[ \t]", "");
        List<Entry> out = new ArrayList<>();
        boolean inEvent = false;
        String uid = null, summary = null, description = "", dtstart = null;

        for (String line : text.split("\n")) {
            if (line.equals("BEGIN:VEVENT")) {
                inEvent = true; uid = null; summary = null; description = ""; dtstart = null;
            } else if (line.equals("END:VEVENT")) {
                if (inEvent && summary != null && dtstart != null) {
                    LocalDate date = parseDate(dtstart, zone);
                    if (date != null) {
                        String cleanSummary = unescape(summary);
                        String id = (uid != null && !uid.isBlank()) ? uid.strip() : cleanSummary + "|" + dtstart;
                        out.add(new Entry(id, cleanSummary, unescape(description), date));
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                int colon = line.indexOf(':');
                if (colon < 0) continue;
                String name = line.substring(0, colon).split(";")[0].toUpperCase();
                String value = line.substring(colon + 1);
                switch (name) {
                    case "UID" -> uid = value;
                    case "SUMMARY" -> summary = value;
                    case "DESCRIPTION" -> description = value;
                    case "DTSTART" -> dtstart = value;
                    default -> { }
                }
            }
        }
        return out;
    }

    // A deadline of 11:59 PM Eastern is stored as the next day in UTC, so convert before taking the date
    private static LocalDate parseDate(String value, ZoneId zone) {
        try {
            if (value.endsWith("Z")) {
                return LocalDateTime.parse(value.substring(0, value.length() - 1), UTC_STAMP)
                        .atZone(ZoneOffset.UTC).withZoneSameInstant(zone).toLocalDate();
            }
            return LocalDate.parse(value.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String unescape(String s) {
        return s.replace("\\n", " ").replace("\\N", " ")
                .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\").strip();
    }
}
