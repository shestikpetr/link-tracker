package backend.academy.linktracker.scrapper.service;

import java.net.URI;
import org.springframework.stereotype.Component;

@Component
public class StackoverflowLinkExtractor {
    public boolean supports(URI url) {
        if (!"stackoverflow.com".equals(url.getHost())) return false;
        String[] parts = url.getPath().split("/");
        if (parts.length < 3 || !"questions".equals(parts[1])) return false;
        try {
            Long.parseLong(parts[2]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public Long extractQuestionId(URI url) {
        String[] parts = url.getPath().split("/");
        return Long.parseLong(parts[2]);
    }
}
