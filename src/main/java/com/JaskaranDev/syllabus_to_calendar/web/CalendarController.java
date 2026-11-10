package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class CalendarController {

    public record Day(LocalDate date, boolean inMonth, boolean today, String holiday, List<RowForm> events) {}

    @GetMapping("/calendar")
    public String calendar(@RequestParam(value = "month", required = false) String month,
                           HttpSession session, Model model) {
        List<RowForm> rows = keptRows(session);
        if (rows.isEmpty()) {
            return "redirect:/";
        }

        YearMonth ym;
        try {
            ym = month == null ? YearMonth.now() : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            ym = YearMonth.now();
        }

        LocalDate gridStart = ym.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate gridEnd = ym.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        Map<LocalDate, String> holidays = new HashMap<>();
        for (int y = gridStart.getYear(); y <= gridEnd.getYear(); y++) {
            holidays.putAll(Holidays.forYear(y));
        }
        Map<LocalDate, List<RowForm>> byDate = rows.stream()
                .collect(Collectors.groupingBy(RowForm::getDate));

        LocalDate today = LocalDate.now();
        List<List<Day>> weeks = new ArrayList<>();
        List<Day> week = new ArrayList<>();
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            week.add(new Day(d, YearMonth.from(d).equals(ym), d.equals(today),
                    holidays.get(d), byDate.getOrDefault(d, List.of())));
            if (week.size() == 7) {
                weeks.add(week);
                week = new ArrayList<>();
            }
        }

        model.addAttribute("weeks", weeks);
        model.addAttribute("monthLabel", ym.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + ym.getYear());
        model.addAttribute("prev", ym.minusMonths(1).toString());
        model.addAttribute("next", ym.plusMonths(1).toString());
        return "calendar";
    }

    // Exports only the events from the user's syllabi (no holidays)
    @GetMapping("/calendar/export.ics")
    public ResponseEntity<byte[]> export(HttpSession session) {
        List<CalendarEvent> events = keptRows(session).stream()
                .map(r -> new CalendarEvent(r.fullTitle(), r.getDate(), r.getType()))
                .toList();
        byte[] body = IcsWriter.write(events).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"deadlines.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(body);
    }

    @SuppressWarnings("unchecked")
    private static List<RowForm> keptRows(HttpSession session) {
        List<RowForm> rows = (List<RowForm>) session.getAttribute(UploadController.ROWS);
        if (rows == null) return List.of();
        return rows.stream().filter(RowForm::isKeep).toList();
    }
}