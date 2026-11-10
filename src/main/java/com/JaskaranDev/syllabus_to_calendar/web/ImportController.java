package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.extract.CourseDetector;
import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Controller
public class ImportController {

    static final String FEED_URL = "feedUrl";
    static final String LAST_SYNC = "lastSync";
    private static final String KNOWN = "knownFeedUids";
    private static final Duration STALE = Duration.ofMinutes(15);

    // Turns UTC timestamps into the student's local date. We'll read this from the browser later.
    private static final ZoneId ZONE = ZoneId.of("America/Toronto");

    private static final Pattern EXAM = Pattern.compile("(?i)\\b(mid-?terms?|exams?|final)\\b");
    private static final Pattern QUIZ = Pattern.compile("(?i)\\bquiz(zes)?\\b");
    private static final Pattern PROJECT = Pattern.compile("(?i)\\b(projects?|essays?|papers?|presentations?|reports?|proposals?)\\b");
    private static final Pattern NOT_A_DEADLINE = Pattern.compile("(?i)\\b(available|availability|opens?|starts?|released?)\\b");

    private static final CourseDetector COURSES = new CourseDetector();

    private record Result(int added, int updated, int removed) {}

    @PostMapping("/import")
    public String importFeed(@RequestParam(value = "feedUrl", required = false) String feedUrl,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             HttpSession session, RedirectAttributes redirect) {
        String ics;
        boolean fromUrl = false;
        try {
            if (file != null && !file.isEmpty()) {
                ics = new String(file.getBytes(), StandardCharsets.UTF_8);
            } else if (feedUrl != null && !feedUrl.isBlank()) {
                ics = fetch(feedUrl);
                fromUrl = true;
            } else {
                redirect.addFlashAttribute("error", "Paste your Brightspace calendar link or choose an .ics file.");
                return "redirect:/";
            }
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        } catch (IOException | InterruptedException e) {
            redirect.addFlashAttribute("error", "Couldn't download that calendar. Check the link and try again.");
            return "redirect:/";
        }

        List<IcsReader.Entry> entries = IcsReader.read(ics, ZONE);
        if (entries.isEmpty()) {
            redirect.addFlashAttribute("error", "No events found in that calendar.");
            return "redirect:/";
        }

        String feedCourse = detectCourse(IcsReader.calendarName(ics));
        Result result = merge(session, entries, feedCourse);
        if (fromUrl) {
            session.setAttribute(FEED_URL, feedUrl.strip());
            session.setAttribute(LAST_SYNC, Instant.now());
        }
        UploadController.sources(session).add("Brightspace (" + result.added() + " events)");
        return "redirect:/review";
    }

    @PostMapping("/sync")
    public String sync(HttpSession session, RedirectAttributes redirect) {
        redirect.addFlashAttribute("syncMessage", syncNow(session));
        return "redirect:/calendar";
    }

    // Called when the calendar opens: re-download the feed if it's been a while
    static void syncIfStale(HttpSession session) {
        if (session.getAttribute(FEED_URL) == null) return;
        Instant last = (Instant) session.getAttribute(LAST_SYNC);
        if (last == null || Duration.between(last, Instant.now()).compareTo(STALE) > 0) {
            syncNow(session);
        }
    }

    static String syncNow(HttpSession session) {
        String url = (String) session.getAttribute(FEED_URL);
        if (url == null) return "No Brightspace link saved.";
        try {
            String ics = fetch(url);
            List<IcsReader.Entry> entries = IcsReader.read(ics, ZONE);
            if (entries.isEmpty()) {
                return "Brightspace returned no events, so nothing was changed.";
            }
            Result r = merge(session, entries, detectCourse(IcsReader.calendarName(ics)));
            session.setAttribute(LAST_SYNC, Instant.now());
            if (r.added() + r.updated() + r.removed() == 0) return "Already up to date.";
            return "Synced: " + r.added() + " new, " + r.updated() + " updated, " + r.removed() + " removed.";
        } catch (Exception e) {
            return "Couldn't reach Brightspace. Showing your last synced events.";
        }
    }

    private static Result merge(HttpSession session, List<IcsReader.Entry> entries, String feedCourse) {
        List<RowForm> rows = UploadController.rows(session);
        Set<String> known = known(session);
        Set<String> seen = new HashSet<>();
        int added = 0, updated = 0;

        for (IcsReader.Entry entry : entries) {
            if (NOT_A_DEADLINE.matcher(entry.summary()).find()) continue;
            String title = entry.summary().replaceAll("(?i)\\s*[-–]?\\s*\\bdue\\b\\s*$", "").strip();
            if (title.isBlank()) continue;

            seen.add(entry.uid());
            String date = entry.date().toString();
            RowForm existing = rows.stream()
                    .filter(r -> entry.uid().equals(r.getFeedUid()))
                    .findFirst().orElse(null);
                String course = detectCourse(entry.summary() + "\n" + entry.description() + "\n"
                    + entry.location() + "\n" + notes(entry));
                if (course.isBlank()) course = feedCourse;
                String notes = notes(entry);

            if (existing != null) {
                boolean untouched = existing.getTitle().equals(existing.getFeedTitle())
                        && existing.getDate().toString().equals(existing.getFeedDate());
                boolean changed = !title.equals(existing.getTitle()) || !entry.date().equals(existing.getDate());
                if (untouched && changed) {
                    existing.setTitle(title);
                    existing.setDate(entry.date());
                    existing.setType(classify(title));
                    updated++;
                }
                existing.setFeedTitle(title);
                existing.setFeedDate(date);
                if ((existing.getCourse() == null || existing.getCourse().isBlank()) && !course.isBlank()) {
                    existing.setCourse(course);
                }
                existing.setDescription(entry.description());
                existing.setUrl(entry.url());
                existing.setNotes(notes);
            } else if (!known.contains(entry.uid())) {
                // never seen before; if we've seen it and it's gone from the rows, the user deleted it
                RowForm row = RowForm.from(new CalendarEvent(title, entry.date(), classify(title)), course);
                row.setDescription(entry.description());
                row.setUrl(entry.url());
                row.setNotes(notes);
                if (!UploadController.alreadyHas(rows, row)) {
                    row.setFeedUid(entry.uid());
                    row.setFeedTitle(title);
                    row.setFeedDate(date);
                    rows.add(row);
                    added++;
                }
                known.add(entry.uid());
            }
        }

        int before = rows.size();
        rows.removeIf(r -> !r.getFeedUid().isEmpty() && !seen.contains(r.getFeedUid()));
        int removed = before - rows.size();

        UploadController.sortRows(rows);
        return new Result(added, updated, removed);
    }

    private static String detectCourse(String text) {
        return COURSES.detect(text).map(code -> code.replace(" ", "")).orElse("");
    }

    private static String notes(IcsReader.Entry entry) {
        String otherProperties = entry.properties().entrySet().stream()
            .filter(property -> !Set.of("UID", "SUMMARY", "DESCRIPTION", "DTSTART", "DTEND",
                    "DTSTAMP", "URL", "LOCATION", "STATUS", "CLASS", "TRANSP", "SEQUENCE",
                        "CREATED", "LAST-MODIFIED", "RECURRENCE-ID").contains(property.getKey()))
            .flatMap(property -> property.getValue().stream()
                        .map(value -> property.getKey() + ": " + value))
            .collect(java.util.stream.Collectors.joining("\n"));
        if (entry.location().isBlank()) return otherProperties;
        return otherProperties.isBlank() ? "Location: " + entry.location()
                : "Location: " + entry.location() + "\n" + otherProperties;
    }

    @SuppressWarnings("unchecked")
    private static Set<String> known(HttpSession session) {
        Set<String> known = (Set<String>) session.getAttribute(KNOWN);
        if (known == null) {
            known = new HashSet<>();
            session.setAttribute(KNOWN, known);
        }
        return known;
    }

    private static EventType classify(String title) {
        if (EXAM.matcher(title).find()) return EventType.EXAM;
        if (QUIZ.matcher(title).find()) return EventType.QUIZ;
        if (PROJECT.matcher(title).find()) return EventType.PROJECT;
        return EventType.ASSIGNMENT;
    }

    private static String fetch(String rawUrl) throws IOException, InterruptedException {
        String url = rawUrl.strip().replaceFirst("(?i)^webcal://", "https://");
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("That doesn't look like a valid link.");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
            throw new IllegalArgumentException("Use the https:// or webcal:// link from Brightspace.");
        }
        // The server downloads this link, so refuse addresses that point back at the server's own network
        for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
            if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()) {
                throw new IllegalArgumentException("That link isn't allowed.");
            }
        }
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }
        if (response.body().length() > 5_000_000) {
            throw new IllegalArgumentException("That calendar is too large.");
        }
        return response.body();
    }
}