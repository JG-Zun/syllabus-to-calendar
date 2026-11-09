package com.JaskaranDev.syllabus_to_calendar.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

    @TempDir
    Path tempDir;

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    private Path createPdf(String fileName, String text) throws IOException {
        Path path = tempDir.resolve(fileName);
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            if (text != null) {
                try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.newLineAtOffset(72, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            doc.save(path.toFile());
        }
        return path;
    }

    @Test
    void extractsTextFromPdf() throws IOException {
        Path pdf = createPdf("syllabus.pdf", "Midterm exam: October 24, 2026");
        String result = extractor.extractText(pdf);
        assertTrue(result.contains("Midterm exam: October 24, 2026"));
    }

    @Test
    void rejectsPdfWithNoText() throws IOException {
        Path pdf = createPdf("blank.pdf", null);
        assertThrows(IOException.class, () -> extractor.extractText(pdf));
    }
}
