package dev.lucas.user_microservice.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D");

    private InputSanitizer() {
    }

    public static String text(String value) {
        if (value == null) {
            return null;
        }
        String clean = HTML_TAGS.matcher(value).replaceAll("");
        clean = CONTROL_CHARS.matcher(clean).replaceAll(" ");
        clean = clean.replace("<", "").replace(">", "");
        return WHITESPACE.matcher(clean).replaceAll(" ").trim();
    }

    public static String email(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static String digits(String value) {
        if (value == null) {
            return null;
        }
        String digits = NON_DIGITS.matcher(value).replaceAll("");
        return digits.isEmpty() ? null : digits;
    }
}
