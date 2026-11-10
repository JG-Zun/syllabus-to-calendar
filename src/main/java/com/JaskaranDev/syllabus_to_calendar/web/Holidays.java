package com.JaskaranDev.syllabus_to_calendar.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

public class Holidays {

    public static Map<LocalDate, String> forYear(int y) {
        Map<LocalDate, String> m = new HashMap<>();
        m.put(LocalDate.of(y, 1, 1), "New Year's Day");
        m.put(LocalDate.of(y, 2, 14), "Valentine's Day");
        m.put(nth(y, 2, DayOfWeek.MONDAY, 3), "Family Day");
        LocalDate easter = easter(y);
        m.put(easter.minusDays(2), "Good Friday");
        m.put(easter, "Easter Sunday");
        m.put(nth(y, 5, DayOfWeek.SUNDAY, 2), "Mother's Day");
        m.put(LocalDate.of(y, 5, 25).with(TemporalAdjusters.previous(DayOfWeek.MONDAY)), "Victoria Day");
        m.put(nth(y, 6, DayOfWeek.SUNDAY, 3), "Father's Day");
        m.put(LocalDate.of(y, 7, 1), "Canada Day");
        m.put(nth(y, 8, DayOfWeek.MONDAY, 1), "Civic Holiday");
        m.put(nth(y, 9, DayOfWeek.MONDAY, 1), "Labour Day");
        m.put(nth(y, 10, DayOfWeek.MONDAY, 2), "Thanksgiving");
        m.put(LocalDate.of(y, 10, 31), "Halloween");
        m.put(LocalDate.of(y, 11, 11), "Remembrance Day");
        m.put(LocalDate.of(y, 12, 24), "Christmas Eve");
        m.put(LocalDate.of(y, 12, 25), "Christmas Day");
        m.put(LocalDate.of(y, 12, 26), "Boxing Day");
        m.put(LocalDate.of(y, 12, 31), "New Year's Eve");
        return m;
    }

    private static LocalDate nth(int year, int month, DayOfWeek day, int n) {
        return LocalDate.of(year, month, 1).with(TemporalAdjusters.dayOfWeekInMonth(n, day));
    }

    // Anonymous Gregorian algorithm
    static LocalDate easter(int y) {
        int a = y % 19, b = y / 100, c = y % 100;
        int d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4, k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(y, month, day);
    }
}