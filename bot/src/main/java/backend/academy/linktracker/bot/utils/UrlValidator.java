package backend.academy.linktracker.bot.utils;

import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class UrlValidator {
    public boolean isValid(String text) {
        try {
            URI uri = URI.create(text.trim());
            return uri.isAbsolute() && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public URI parse(String text) {
        return URI.create(text.trim());
    }
}
