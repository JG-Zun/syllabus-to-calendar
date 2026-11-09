package com.JaskaranDev.syllabus_to_calendar.pipeline;

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

    public record Result(List<CalendarEvent> events, Redactor.ContactInfo contact) {}

    private final PdfTextExtractor pdfReader = new PdfTextExtractor();
    private final Redactor redactor = new Redactor();
    private final Extractor extractor;

    public SyllabusProcessor(Extractor extractor) {
        this.extractor = extractor;
    }

    public Result process(Path pdf, LocalDate termStart) throws IOException {
        String text = pdfReader.extractText(pdf);
        Redactor.Result redacted = redactor.redact(text);
        List<CalendarEvent> events = extractor.extract(redacted.text(), termStart).stream()
                .sorted(Comparator.comparing(CalendarEvent::date))
                .toList();
        return new Result(events, redacted.contact());
    }
}
