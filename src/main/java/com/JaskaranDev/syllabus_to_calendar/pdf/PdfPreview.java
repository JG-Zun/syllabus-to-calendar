package com.JaskaranDev.syllabus_to_calendar.pdf;

import com.JaskaranDev.syllabus_to_calendar.privacy.Redactor;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PdfPreview {

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: pass ONE argument, the path to a folder of PDFs.");
            return;
        }
        Path folder = Path.of(args[0]);
        Path rawDir = folder.resolve("extracted");
        Path redactedDir = folder.resolve("redacted");
        Files.createDirectories(rawDir);
        Files.createDirectories(redactedDir);

        PdfTextExtractor extractor = new PdfTextExtractor();
        Redactor redactor = new Redactor();

        try (DirectoryStream<Path> pdfs = Files.newDirectoryStream(folder, "*.pdf")) {
            for (Path pdf : pdfs) {
                System.out.println("==== " + pdf.getFileName() + " ====");
                try {
                    String text = extractor.extractText(pdf);
                    Redactor.Result redacted = redactor.redact(text);
                    String baseName = pdf.getFileName().toString().replaceFirst("(?i)\\.pdf$", "");
                    Files.writeString(rawDir.resolve(baseName + ".txt"), text);
                    Files.writeString(redactedDir.resolve(baseName + ".txt"), redacted.text());
                    System.out.println("Characters: " + text.length()
                            + " | emails redacted: " + redacted.emailsRedacted()
                            + " | phones redacted: " + redacted.phonesRedacted()
                            + " | lines dropped: " + redacted.linesDropped());
                } catch (IOException e) {
                    System.out.println("FAILED: " + e.getMessage());
                }
                System.out.println();
            }
        }
    }
}