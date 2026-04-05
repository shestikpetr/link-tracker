package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackoverflowLinkExtractor {
    private final StackoverflowClient stackoverflowClient;

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

    public String fetchQuestionTitle(Long questionId) {
        var items = stackoverflowClient.getQuestions(questionId).items();
        if (items.isEmpty()) {
            return "(неизвестный вопрос)";
        }
        return items.getFirst().title();
    }
}
