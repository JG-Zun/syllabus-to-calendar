package com.JaskaranDev.syllabus_to_calendar.pipeline;

import com.JaskaranDev.syllabus_to_calendar.extract.CourseDetector;
import com.JaskaranDev.syllabus_to_calendar.extract.Extractor;
import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.pdf.PdfTextExtractor;
import com.JaskaranDev.syllabus_to_calendar.privacy.Redactor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public class SyllabusProcessor {

    public record Result(String course, List<CalendarEvent> events, Redactor.ContactInfo contact) {}

    private final PdfTextExtractor pdfReader = new PdfTextExtractor();
    private final Redactor redactor = new Redactor();
    private final CourseDetector courseDetector = new CourseDetector();
    private final Extractor extractor;

    public SyllabusProcessor(Extractor extractor) {
        this.extractor = extractor;
    }

    public Result process(Path pdf, LocalDate termStart) throws IOException {
        String text = pdfReader.extractText(pdf);
        Redactor.Result redacted = redactor.redact(text);
        String course = courseDetector.detect(redacted.text()).orElse(fileLabel(pdf));

        List<CalendarEvent> events = extractor.extract(redacted.text(), termStart).stream()
                .map(e -> new CalendarEvent(course + ": " + e.title(), e.date(), e.type()))
                .sorted(Comparator.comparing(CalendarEvent::date))
                .toList();
        return new Result(course, events, redacted.contact());
    }

    private static String fileLabel(Path pdf) {
        return pdf.getFileName().toString().replaceFirst("(?i)\\.pdf$", "");
    }
}