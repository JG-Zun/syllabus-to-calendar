package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDate;

public class RowForm implements Serializable {

    private String title;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private EventType type;
    private boolean keep = true;

    public RowForm() {}

    public static RowForm from(CalendarEvent event) {
        RowForm row = new RowForm();
        row.title = event.title();
        row.date = event.date();
        row.type = event.type();
        return row;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public boolean isKeep() { return keep; }
    public void setKeep(boolean keep) { this.keep = keep; }
}
