package com.JaskaranDev.syllabus_to_calendar.model;

import java.time.LocalDate;
import java.util.Objects;

public record CalendarEvent(String title, LocalDate date, EventType type) {

    public CalendarEvent {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(type, "type");
        title = title.strip();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title must not be blank");
        }

    }
}
