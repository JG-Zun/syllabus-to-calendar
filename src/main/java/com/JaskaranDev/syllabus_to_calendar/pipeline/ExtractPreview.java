package com.JaskaranDev.syllabus_to_calendar.pipeline;

import com.JaskaranDev.syllabus_to_calendar.export.IcsExporter;
import com.JaskaranDev.syllabus_to_calendar.extract.RuleBasedExtractor;
import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

public class ExtractPreview {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: pass TWO arguments: the PDF folder path and the term start date (yyyy-MM-dd).");
            return;
        }
        Path folder = Path.of(args[0]);
        LocalDate termStart = LocalDate.parse(args[1]);
        Path calendarDir = folder.resolve("calendars");
        Files.createDirectories(calendarDir);

        SyllabusProcessor processor = new SyllabusProcessor(new RuleBasedExtractor());
        IcsExporter exporter = new IcsExporter();

        try (DirectoryStream<Path> pdfs = Files.newDirectoryStream(folder, "*.pdf")) {
            for (Path pdf : pdfs) {
                System.out.println("==== " + pdf.getFileName() + " ====");
                try {
                    SyllabusProcessor.Result result = processor.process(pdf, termStart);
                    System.out.println("Course" + result.course());
                    for (CalendarEvent event : result.events()) {
                        System.out.println(event.date() + "  " + event.type() + "  " + event.title());
                    }
                    System.out.println(result.events().size() + " events found; "
                            + result.contact().contactLines().size() + " contact lines kept aside");
                    if (!result.events().isEmpty()) {
                        String baseName = pdf.getFileName().toString().replaceFirst("(?i)\\.pdf$", "");
                        exporter.exportToFile(result.events(), calendarDir.resolve(baseName + ".ics"));
                    }
                } catch (IOException e) {
                    System.out.println("FAILED: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }
}