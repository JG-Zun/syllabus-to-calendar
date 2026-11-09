package com.JaskaranDev.syllabus_to_calendar.extract;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface Extractor {

    String name();

    List<CalendarEvent> extract(String text, LocalDate termStart) throws IOException;
}
