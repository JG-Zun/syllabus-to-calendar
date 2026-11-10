package com.JaskaranDev.syllabus_to_calendar.web;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HolidaysTest {
    @Test void easter2026() {
        assertEquals(LocalDate.of(2026, 4, 5), Holidays.easter(2026));
    }
    @Test void thanksgiving2026() {
        assertEquals("Thanksgiving", Holidays.forYear(2026).get(LocalDate.of(2026, 10, 12)));
    }
    @Test void boxingDay() {
        assertEquals("Boxing Day", Holidays.forYear(2026).get(LocalDate.of(2026, 12, 26)));
    }
}