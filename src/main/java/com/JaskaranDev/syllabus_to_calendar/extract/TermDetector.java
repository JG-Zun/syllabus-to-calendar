package com.JaskaranDev.syllabus_to_calendar.extract;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TermDetector {

    public record DetectedTerm(LocalDate start, String description) {}

    private static final int HEADER_CHARS = 3000;
    private static final String MONTH = "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?"
            + "|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)";
    private static final List<String> MONTH_PREFIXES = List.of(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");
    private static final String SEASON = "(fall|autumn|winter|spring|summer)";

    private static final Pattern RANGE = Pattern.compile(
            "(?i)\\b" + MONTH + "\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(20\\d{2}))?"
                    + "\\s*(?:-|–|—|to|until)\\s*"
                    + MONTH + "\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(20\\d{2}))?\\b");
    private static final Pattern SEASON_YEAR = Pattern.compile(
            "(?i)\\b" + SEASON + "\\s+(?:term\\s+|semester\\s+)?(20\\d{2})\\b");
    private static final Pattern YEAR_SEASON = Pattern.compile(
            "(?i)\\b(20\\d{2})\\s*[-,]?\\s*" + SEASON + "\\b");
    private static final Pattern SEASON_ONLY = Pattern.compile(
            "(?i)\\b" + SEASON + "\\s+(?:term|semester|session)\\b");
    private static final Pattern YEAR = Pattern.compile("\\b(20\\d{2})\\b");

    private static final DateTimeFormatter PRETTY = DateTimeFormatter.ofPattern("MMM d, yyyy");

    public static Optional<DetectedTerm> detect(String text) {
        String header = text.substring(0, Math.min(text.length(), HEADER_CHARS));
        Matcher yearMatcher = YEAR.matcher(header);
        Integer headerYear = yearMatcher.find() ? Integer.parseInt(yearMatcher.group(1)) : null;

        Matcher range = RANGE.matcher(header);
        while (range.find()) {
            int startMonth = monthNumber(range.group(1));
            int endMonth = monthNumber(range.group(4));
            if (startMonth == endMonth) {
                continue;
            }
            int year = range.group(3) != null ? Integer.parseInt(range.group(3))
                    : headerYear != null ? headerYear : LocalDate.now().getYear();
            int endYear = range.group(6) != null ? Integer.parseInt(range.group(6))
                    : endMonth < startMonth ? year + 1 : year;
            LocalDate start = safeDate(year, startMonth, Integer.parseInt(range.group(2)));
            LocalDate end = safeDate(endYear, endMonth, Integer.parseInt(range.group(5)));
            if (start == null || end == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(start, end);
            if (days >= 60 && days <= 200) {
                return Optional.of(new DetectedTerm(start,
                        start.format(PRETTY) + " to " + end.format(PRETTY)));
            }
        }

        Matcher seasonYear = SEASON_YEAR.matcher(header);
        if (seasonYear.find()) {
            return Optional.of(season(seasonYear.group(1), Integer.parseInt(seasonYear.group(2))));
        }
        Matcher yearSeason = YEAR_SEASON.matcher(header);
        if (yearSeason.find()) {
            return Optional.of(season(yearSeason.group(2), Integer.parseInt(yearSeason.group(1))));
        }
        Matcher seasonOnly = SEASON_ONLY.matcher(header);
        if (seasonOnly.find() && headerYear != null) {
            return Optional.of(season(seasonOnly.group(1), headerYear));
        }
        return Optional.empty();
    }

    private static DetectedTerm season(String name, int year) {
        String s = name.toLowerCase();
        LocalDate start = switch (s) {
            case "fall", "autumn" -> LocalDate.of(year, 9, 1);
            case "summer" -> LocalDate.of(year, 5, 1);
            default -> LocalDate.of(year, 1, 1);   // winter / spring
        };
        return new DetectedTerm(start, Character.toUpperCase(s.charAt(0)) + s.substring(1) + " " + year);
    }

    private static int monthNumber(String text) {
        return MONTH_PREFIXES.indexOf(text.substring(0, 3).toLowerCase()) + 1;
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }
}