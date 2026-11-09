package com.JaskaranDev.syllabus_to_calendar.privacy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Redactor {

    public record ContactInfo(List<String> contactLines, List<String> emails, List<String> phones) {}

    public record Result(String text, ContactInfo contact) {
        public int emailsRedacted() { return contact.emails().size(); }
        public int phonesRedacted() { return contact.phones().size(); }
        public int linesDropped() { return contact.contactLines().size(); }
    }

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}");

    private static final Pattern PHONE =
            Pattern.compile("(?<!\\d)(?:\\+?1[\\s.-]?)?(?:\\(\\d{3}\\)|\\d{3})[\\s.-]?\\d{3}[\\s.-]?\\d{4}(?!\\d)");

    private static final Pattern OFFICE_HOURS = Pattern.compile("(?i)office\\s+hours?");

    private static final Pattern CONTACT_LABEL = Pattern.compile(
            "(?i)^\\s*(?:[-•*]\\s*)?(?:instructors?|professors?|prof\\.?|teaching\\s+assistants?|tas?|contact|office|e-?mail|phone|tel(?:ephone)?)\\s*[:\\-–].*$");

    public Result redact(String text) {
        StringBuilder out = new StringBuilder();
        List<String> contactLines = new ArrayList<>();
        List<String> emails = new ArrayList<>();
        List<String> phones = new ArrayList<>();

        for (String line : text.split("\\R", -1)) {
            if (OFFICE_HOURS.matcher(line).find() || CONTACT_LABEL.matcher(line).matches()) {
                contactLines.add(line.strip());
                continue;
            }
            line = replaceAndCollect(EMAIL, line, "[EMAIL]", emails);
            line = replaceAndCollect(PHONE, line, "[PHONE]", phones);
            out.append(line).append('\n');
        }
        ContactInfo contact = new ContactInfo(
                List.copyOf(contactLines), List.copyOf(emails), List.copyOf(phones));
        return new Result(out.toString(), contact);
    }

    private static String replaceAndCollect(Pattern pattern, String line,
                                            String placeholder, List<String> found) {
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            found.add(matcher.group().strip());
        }
        return pattern.matcher(line).replaceAll(placeholder);
    }
}
