package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.extract.Extractor;
import com.JaskaranDev.syllabus_to_calendar.extract.RuleBasedExtractor;
import com.JaskaranDev.syllabus_to_calendar.extract.TermDetector;
import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;


import jakarta.servlet.http.HttpSession;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class UploadController {

    private static final String ROWS = "rows";
    private static final String SOURCES = "sources";

    private final Extractor extractor = new RuleBasedExtractor();

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        model.addAttribute("count", rows(session).size());
        return "upload";
    }

    @PostMapping("/review")
    public String upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "termStart", required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate termStart,
                         HttpSession session, Model model) throws IOException {
        String text;
        try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
            text = new PDFTextStripper().getText(doc);
        } catch (IOException e) {
            return uploadError("Couldn't read that file. Please upload a PDF.", session, model);
        }

        String termLabel;
        if (termStart != null) {
            termLabel = "term start " + termStart;
        } else {
            var detected = TermDetector.detect(text);
            if (detected.isEmpty()) {
                return uploadError("Couldn't find the term in this syllabus. Please enter the term start date.",
                        session, model);
            }
            termStart = detected.get().start();
            termLabel = detected.get().description();
        }

        List<RowForm> rows = rows(session);
        for (CalendarEvent event : extractor.extract(text, termStart)) {
            if (!alreadyHas(rows, event)) {
                rows.add(RowForm.from(event));
            }
        }
        sortRows(rows);
        sources(session).add(file.getOriginalFilename() + " (" + termLabel + ")");
        return "redirect:/review";
    }

    @GetMapping("/review")
    public String review(HttpSession session, Model model) {
        List<RowForm> rows = rows(session);
        if (rows.isEmpty()) {
            return "redirect:/";
        }
        ReviewForm form = new ReviewForm();
        form.setRows(rows);
        model.addAttribute("form", form);
        model.addAttribute("types", EventType.values());
        model.addAttribute("sources", sources(session));
        return "review";
    }

    // The "Add" button: save any edits, then go upload another syllabus
    @PostMapping("/review/add-another")
    public String addAnother(@ModelAttribute ReviewForm form, BindingResult errors, HttpSession session) {
        saveEdits(form, session);
        return "redirect:/";
    }

    @PostMapping("/review/clear")
    public String clear(HttpSession session) {
        session.removeAttribute(ROWS);
        session.removeAttribute(SOURCES);
        return "redirect:/";
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute ReviewForm form, BindingResult errors,
                                         HttpSession session) {
        List<CalendarEvent> events = saveEdits(form, session).stream()
                .filter(RowForm::isKeep)
                .map(r -> new CalendarEvent(r.getTitle(), r.getDate(), r.getType()))
                .toList();
        byte[] body = IcsWriter.write(events).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"deadlines.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .body(body);
    }

    private String uploadError(String message, HttpSession session, Model model) {
        model.addAttribute("error", message);
        model.addAttribute("count", rows(session).size());
        return "upload";
    }

    // Removes rows the user deleted or left incomplete, sorts, and stores the result
    private List<RowForm> saveEdits(ReviewForm form, HttpSession session) {
        List<RowForm> cleaned = new ArrayList<>();
        for (RowForm row : form.getRows()) {
            if (row != null && row.getDate() != null && row.getType() != null
                    && row.getTitle() != null && !row.getTitle().isBlank()) {
                row.setTitle(row.getTitle().strip());
                cleaned.add(row);
            }
        }
        sortRows(cleaned);
        session.setAttribute(ROWS, cleaned);
        return cleaned;
    }

    private static void sortRows(List<RowForm> rows) {
        rows.sort(Comparator.comparing(RowForm::getDate).thenComparing(RowForm::getTitle));
    }

    private static boolean alreadyHas(List<RowForm> rows, CalendarEvent event) {
        return rows.stream().anyMatch(r -> r.getDate().equals(event.date())
                && r.getType() == event.type()
                && r.getTitle().equalsIgnoreCase(event.title()));
    }

    @SuppressWarnings("unchecked")
    private static List<RowForm> rows(HttpSession session) {
        List<RowForm> rows = (List<RowForm>) session.getAttribute(ROWS);
        if (rows == null) {
            rows = new ArrayList<>();
            session.setAttribute(ROWS, rows);
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static List<String> sources(HttpSession session) {
        List<String> sources = (List<String>) session.getAttribute(SOURCES);
        if (sources == null) {
            sources = new ArrayList<>();
            session.setAttribute(SOURCES, sources);
        }
        return sources;
    }
}