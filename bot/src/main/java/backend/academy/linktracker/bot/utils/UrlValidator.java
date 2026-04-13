package backend.academy.linktracker.bot.utils;

import java.net.URI;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UrlValidator {
    public Optional<URI> parse(String text) {
        try {
            URI uri = URI.create(text.trim());
            if (uri.isAbsolute() && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
                return Optional.of(uri);
            }
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
