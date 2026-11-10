package com.JaskaranDev.syllabus_to_calendar.web;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.net.URI;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RowForm implements Serializable {

    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s<>\\\"]+", Pattern.CASE_INSENSITIVE);

    private String title;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;
    private EventType type;
    private boolean keep = true;

    private String feedUid = "";
    private String feedTitle = "";
    private String feedDate = "";
    private String description = "";
    private String url = "";
    private String notes = "";

    public String getFeedUid() { return feedUid; }
    public void setFeedUid(String feedUid) { this.feedUid = feedUid; }
    public String getFeedTitle() { return feedTitle; }
    public void setFeedTitle(String feedTitle) { this.feedTitle = feedTitle; }
    public String getFeedDate() { return feedDate; }
    public void setFeedDate(String feedDate) { this.feedDate = feedDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description == null ? "" : description; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url == null ? "" : url; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes == null ? "" : notes; }

    public RowForm() {}

    private String course = "";

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public static RowForm from(CalendarEvent event, String course) {
        RowForm row = new RowForm();
        row.title = event.title();
        row.date = event.date();
        row.type = event.type();
        row.course = course;
        return row;
    }

    public String fullTitle() {
        return title;
    }

    public List<String> getLinks() {
        Set<String> links = new LinkedHashSet<>();
        addSafeLink(links, url);
        addLinksFrom(links, description);
        addLinksFrom(links, notes);
        return List.copyOf(links);
    }

    private static void addLinksFrom(Set<String> links, String text) {
        Matcher matcher = URL_PATTERN.matcher(text == null ? "" : text);
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("[),.;]+$", "");
            addSafeLink(links, candidate);
        }
    }

    private static void addSafeLink(Set<String> links, String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value.strip());
            if (uri.getHost() != null && ("https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme()))) {
                links.add(uri.toASCIIString());
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore malformed or non-web links in untrusted calendar metadata.
        }
    }

    public int hue() {
        return Math.abs(java.util.Objects.hash(course == null ? "" : course) % 360);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public boolean isKeep() { return keep; }
    public void setKeep(boolean keep) { this.keep = keep; }
}
