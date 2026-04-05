package backend.academy.linktracker.scrapper.utils;

import org.springframework.stereotype.Component;

@Component
public class TextUtils {
    public static final int PREVIEW_MAX_LENGTH = 200;

    public String truncate(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.length() <= PREVIEW_MAX_LENGTH ? text : text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
}
