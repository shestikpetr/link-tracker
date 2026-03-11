package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackoverflowLinkHandler implements LinkHandler {
    private final StackoverflowClient stackoverflowClient;

    @Override
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

    @Override
    public Instant getLastActivity(URI url) {
        String[] parts = url.getPath().split("/");
        var items = stackoverflowClient.getQuestions(Long.parseLong(parts[2])).items();
        if (items.isEmpty()) {
            throw new IllegalStateException("API не вернул вопросы для ссылки: " + url);
        }
        return items.getFirst().lastActivityDate();
    }
}
