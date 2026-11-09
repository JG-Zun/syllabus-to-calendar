package com.JaskaranDev.syllabus_to_calendar.privacy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Redactor {

    public record Result(String text, int emailsRedacted, int phonesRedacted, int linesDropped) {}

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    private static final Pattern PHONE =
            Pattern.compile("(?<!\\d)(?:\\+?1[\\s.-]?)?(?:\\(\\d{3}\\)|\\d{3})[\\s.-]?\\d{3}[\\s.-]?\\d{4}(?!\\d)");

    private static final Pattern OFFICE_HOURS = Pattern.compile("(?i)office\\s+hours?");

    private static final Pattern CONTACT_LABEL = Pattern.compile(
            "(?i)^\\s*(?:[-•*]\\s*)?(?:instructors?|professors?|prof\\.?|teaching\\s+assistants?|tas?|contact|office|e-?mail|phone|tel(?:ephone)?)\\s*[:\\-–].*$");

    public Result redact(String text) {
        StringBuilder out = new StringBuilder();
        int emails = 0;
        int phones = 0;
        int dropped = 0;

        for (String line : text.split("\\R", -1)) {
            if (OFFICE_HOURS.matcher(line).find() || CONTACT_LABEL.matcher(line).matches()) {
                dropped++;
                continue;
            }
            emails += count(EMAIL.matcher(line));
            line = EMAIL.matcher(line).replaceAll("[EMAIL]");
            phones += count(PHONE.matcher(line));
            line = PHONE.matcher(line).replaceAll("[PHONE]");
            out.append(line).append('\n');
        }
        return new Result(out.toString(), emails, phones, dropped);
    }

    private static int count(Matcher matcher) {
        int n = 0;
        while (matcher.find()) {
            n++;
        }
        return n;
    }
}
