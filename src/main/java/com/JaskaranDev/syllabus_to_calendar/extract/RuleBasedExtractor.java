package com.JaskaranDev.syllabus_to_calendar.extract;

import com.JaskaranDev.syllabus_to_calendar.model.CalendarEvent;
import com.JaskaranDev.syllabus_to_calendar.model.EventType;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RuleBasedExtractor implements Extractor {

    private static final String MONTH = "(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?"
            + "|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)";
    private static final String WEEKDAY = "(?:(?:mon(?:day)?|tue(?:s(?:day)?)?|wed(?:nesday)?"
            + "|thu(?:r(?:s(?:day)?)?)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\\.?,?\\s+)?";
    private static final List<String> MONTH_PREFIXES = List.of(
            "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");

    private static final Pattern ISO_DATE = Pattern.compile("\\b(20\\d{2})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern MONTH_FIRST = Pattern.compile(
            "(?i)\\b" + WEEKDAY + MONTH + "\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?\\b(?:,?\\s+(20\\d{2})\\b)?");
    private static final Pattern DAY_FIRST = Pattern.compile(
            "(?i)\\b" + WEEKDAY + "(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?" + MONTH + "\\b\\.?(?:,?\\s+(20\\d{2})\\b)?");

    private record KeywordRule(EventType type, Pattern pattern) {}

    private static final List<KeywordRule> RULES = List.of(
            new KeywordRule(EventType.EXAM,
                    Pattern.compile("(?i)\\b(?:mid-?terms?|exams?|examination|tests?)\\b")),
            new KeywordRule(EventType.QUIZ,
                    Pattern.compile("(?i)\\bquiz(?:zes|es)?\\b")),
            new KeywordRule(EventType.PROJECT,
                    Pattern.compile("(?i)\\b(?:projects?|presentations?|proposals?|essays?(?!\\s+questions?)|papers?|reports?|thesis)\\b")),
            new KeywordRule(EventType.ASSIGNMENT,
                    Pattern.compile("(?i)\\b(?:assignments?|homework|hw\\d*|problem\\s+sets?|psets?|labs?|worksheets?|deliverables?)\\b")));

    private static final Pattern DUE = Pattern.compile("(?i)\\b(?:due|deadline|submit(?:ted)?|submission)\\b");
    private static final Pattern IGNORE = Pattern.compile(
            "(?i)\\bno\\s+(?:class|lecture|tutorial)|\\bholidays?\\b|\\breading\\s+week\\b|\\bbreak\\b|\\bcancell?ed\\b"
                    + "|\\b(?:mid-?term|exam|final|quiz|test)\\s+review\\b|\\breview\\s+(?:session|for)\\b");
    private static final Pattern RELEASE = Pattern.compile(
            "(?i)\\b(?:released?|posted|assigned|handed\\s+out)\\b");

    private static final Pattern SEGMENT_SPLIT = Pattern.compile(";|(?<=[.!?])\\s+(?=[A-Z])");
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private record DateMatch(int start, int end, LocalDate date) {}

    @Override
    public String name() {
        return "rule-based";
    }

    @Override
    public List<CalendarEvent> extract(String text, LocalDate termStart) {
        Set<CalendarEvent> events = new LinkedHashSet<>();
        for (String line : text.split("\\R")) {
            for (String segment : SEGMENT_SPLIT.split(line)) {
                CalendarEvent event = parseSegment(segment, termStart);
                if (event != null) {
                    events.add(event);
                }
            }
        }
        return mergeDuplicates(events);
    }

    private static List<CalendarEvent> mergeDuplicates(Set<CalendarEvent> events) {
        List<CalendarEvent> merged = new ArrayList<>();
        for (CalendarEvent event : events) {
            boolean matched = false;
            for (int i = 0; i < merged.size(); i++) {
                if (sameEvent(merged.get(i), event)) {
                    if (score(event) < score(merged.get(i))) {
                        merged.set(i, event);
                    }
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                merged.add(event);
            }
        }
        return List.copyOf(merged);
    }

    private static boolean sameEvent(CalendarEvent a, CalendarEvent b) {
        if (!a.date().equals(b.date()) || a.type() != b.type()) {
            return false;
        }
        if (!firstWordStem(a.title()).equals(firstWordStem(b.title()))) {
            return false;
        }
        String numberA = firstNumber(a.title());
        String numberB = firstNumber(b.title());
        return numberA.isEmpty() || numberB.isEmpty() || numberA.equals(numberB);
    }

    private static String firstWordStem(String title) {
        String first = title.toLowerCase().split("\\s+")[0].replaceAll("[^a-z0-9]", "");
        return first.endsWith("s") ? first.substring(0, first.length() - 1) : first;
    }

    private static String firstNumber(String title) {
        Matcher matcher = NUMBER.matcher(title);
        return matcher.find() ? matcher.group() : "";
    }

    private static int score(CalendarEvent event) {
        boolean hasNumber = !firstNumber(event.title()).isEmpty();
        return (hasNumber ? 0 : 1000) + event.title().length();
    }

    private CalendarEvent parseSegment(String segment, LocalDate termStart) {
        if (IGNORE.matcher(segment).find()) {
            return null;
        }
        if (RELEASE.matcher(segment).find() && !DUE.matcher(segment).find()) {
            return null;
        }
        List<DateMatch> dates = findDates(segment, termStart);
        if (dates.isEmpty()) {
            return null;
        }
        EventType type = classify(segment);
        if (type == null) {
            return null;
        }
        DateMatch chosen = chooseDate(segment, dates);
        return new CalendarEvent(cleanTitle(segment, dates, type), chosen.date(), type);
    }

    private static List<DateMatch> findDates(String segment, LocalDate termStart) {
        List<DateMatch> found = new ArrayList<>();

        Matcher iso = ISO_DATE.matcher(segment);
        while (iso.find()) {
            tryAdd(found, iso.start(), iso.end(), safeDate(
                    Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3))));
        }

        Matcher monthFirst = MONTH_FIRST.matcher(segment);
        while (monthFirst.find()) {
            tryAdd(found, monthFirst.start(), monthFirst.end(), resolve(
                    monthNumber(monthFirst.group(1)), Integer.parseInt(monthFirst.group(2)),
                    monthFirst.group(3), termStart));
        }

        Matcher dayFirst = DAY_FIRST.matcher(segment);
        while (dayFirst.find()) {
            tryAdd(found, dayFirst.start(), dayFirst.end(), resolve(
                    monthNumber(dayFirst.group(2)), Integer.parseInt(dayFirst.group(1)),
                    dayFirst.group(3), termStart));
        }

        found.sort(Comparator.comparingInt(DateMatch::start));
        return found;
    }

    private static void tryAdd(List<DateMatch> found, int start, int end, LocalDate date) {
        if (date == null) {
            return;
        }
        for (DateMatch existing : found) {
            if (start < existing.end() && existing.start() < end) {
                return;
            }
        }
        found.add(new DateMatch(start, end, date));
    }

    private static int monthNumber(String monthText) {
        return MONTH_PREFIXES.indexOf(monthText.substring(0, 3).toLowerCase()) + 1;
    }

    private static LocalDate safeDate(int year, int month, int day) {
        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static LocalDate resolve(int month, int day, String yearText, LocalDate termStart) {
        if (yearText != null) {
            return safeDate(Integer.parseInt(yearText), month, day);
        }
        LocalDate candidate = safeDate(termStart.getYear(), month, day);
        if (candidate == null) {
            return null;
        }
        return candidate.isBefore(termStart.minusDays(30)) ? candidate.plusYears(1) : candidate;
    }

    private static EventType classify(String segment) {
        EventType best = null;
        int bestPosition = Integer.MAX_VALUE;
        for (KeywordRule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(segment);
            if (matcher.find() && matcher.start() < bestPosition) {
                best = rule.type();
                bestPosition = matcher.start();
            }
        }
        if (best != null) {
            return best;
        }
        return DUE.matcher(segment).find() ? EventType.OTHER : null;
    }

    private static DateMatch chooseDate(String segment, List<DateMatch> dates) {
        Matcher due = DUE.matcher(segment);
        if (due.find()) {
            for (DateMatch date : dates) {
                if (date.start() >= due.start()) {
                    return date;
                }
            }
        }
        return dates.get(0);
    }

    private static String cleanTitle(String segment, List<DateMatch> dates, EventType type) {
        StringBuilder sb = new StringBuilder(segment);
        for (int i = dates.size() - 1; i >= 0; i--) {
            sb.replace(dates.get(i).start(), dates.get(i).end(), " ");
        }
        String title = sb.toString()
                .replaceAll("\\(\\s*\\)", " ")
                .replaceAll("\\([\\d\\s]*[,\\-–][\\d,\\-–\\s]*\\)", " ")
                .replaceAll("(?i)^\\s*week\\s*\\d+\\s*[-–—:|]*", " ")
                .replaceAll("(?i)^\\s*\\d{1,2}\\s+(?=[a-z])", " ")
                .replaceAll("\\s*[-–—:|,]\\s*(?:[-–—:|,]\\s*)+", " - ")
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s\\-–—:|,;•*.]+|[\\s\\-–—:|,;•*.]+$", "")
                .replaceAll("(?i)\\s+(?:on|by|at|before|until|due)$", "");
        title = trimDescription(title);
        if (title.length() > 100) {
            title = title.substring(0, 100).strip();
        }
        return title.isBlank() ? defaultTitle(type) : title;
    }

    private static String trimDescription(String title) {
        int colon = title.indexOf(':');
        if (colon > 0 && !Character.isDigit(title.charAt(colon - 1))) {
            String after = title.substring(colon + 1).strip();
            if (after.split("\\s+").length >= 6) {
                return title.substring(0, colon).strip();
            }
        }
        return title;
    }

    private static String defaultTitle(EventType type) {
        return switch (type) {
            case EXAM -> "Exam";
            case QUIZ -> "Quiz";
            case ASSIGNMENT -> "Assignment";
            case PROJECT -> "Project";
            case OTHER -> "Deadline";
        };
    }
}