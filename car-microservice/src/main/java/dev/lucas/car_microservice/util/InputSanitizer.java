package dev.lucas.car_microservice.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class InputSanitizer {

    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern CONTROL_CHARS = Pattern.compile("\\p{Cntrl}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

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

    public static String plate(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
