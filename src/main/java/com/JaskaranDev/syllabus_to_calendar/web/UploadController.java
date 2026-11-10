package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.extract.Extractor;
import com.JaskaranDev.syllabus_to_calendar.extract.RuleBasedExtractor;

import com.JaskaranDev.syllabus_to_calendar.extract.TermDetector;
import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@Controller
public class UploadController {

    private final Extractor extractor = new RuleBasedExtractor();

    @GetMapping("/")
    public String home() {
        return "upload";
    }

    @PostMapping("/review")
    public String review(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "termStart", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate termStart,
                         Model model) throws IOException {
        String text;
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            text = new PDFTextStripper().getText(doc);
        }

        String termLabel;
        if (termStart != null) {
            termLabel = "entered manually";
        } else {
            var detected = TermDetector.detect(text);
            if (detected.isEmpty()) {
                model.addAttribute("error",
                        "Couldn't find the term in this syllabus. Please enter the term start date.");
                return "upload";
            }
            termStart = detected.get().start();
            termLabel = detected.get().description();
        }

        List<CalendarEvent> events = new ArrayList<>(extractor.extract(text, termStart));
        events.sort(Comparator.comparing(CalendarEvent::date)
                .thenComparing(CalendarEvent::title));

        model.addAttribute("events", events);
        model.addAttribute("termStart", termStart);
        model.addAttribute("termLabel", termLabel);
        return "review";
    }
}
