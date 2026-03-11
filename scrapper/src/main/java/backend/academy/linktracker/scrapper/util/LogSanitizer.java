package backend.academy.linktracker.scrapper.util;

public final class LogSanitizer {
    private LogSanitizer() {}

    public static String sanitize(Object value) {
        if (value == null) {
            return "null";
        }
        return value.toString().replaceAll("[\\r\\n]", "_");
    }
}
