package com.JaskaranDev.syllabus_to_calendar.extract;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CourseDetector {

    private static final int LINES_TO_SCAN = 30;

    private static final Pattern COURSE_CODE = Pattern.compile(
            "\\b([A-Z]{2,4})[ -]?(?!(?:19|20)\\d{2}\\b)(\\d{3,4}[A-Z]?)\\b");

    public Optional<String> detect(String text) {
        int scanned = 0;
        for (String line : text.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            scanned++;
            if (scanned > LINES_TO_SCAN) {
                break;
            }
            Matcher matcher = COURSE_CODE.matcher(line);
            if (matcher.find()) {
                return Optional.of(matcher.group(1) + " " + matcher.group(2));
            }
        }
        return Optional.empty();
    }
}