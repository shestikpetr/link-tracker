package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.service.formatter.StackoverflowUpdateFormatter;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackoverflowLinkHandler implements LinkHandler {
    private static final String FILTER_WITH_BODY = "withbody";

    private final StackoverflowClient stackoverflowClient;
    private final StackoverflowUpdateFormatter formatter;
    private final StackoverflowLinkExtractor extractor;

    @Override
    public boolean supports(URI url) {
        return extractor.supports(url);
    }

    @Override
    public List<LinkUpdateInfo> checkUpdates(URI url, Instant lastCheckedAt) {
        Long questionId = extractor.extractQuestionId(url);
        String questionTitle = extractor.fetchQuestionTitle(questionId);

        List<LinkUpdateInfo> updates = new ArrayList<>();

        var answers = stackoverflowClient.getAnswers(questionId, "desc", "creation", "stackoverflow", FILTER_WITH_BODY);
        for (var answer : answers.items()) {
            if (answer.creationDate().isAfter(lastCheckedAt)) {
                updates.add(formatter.formatAnswer(questionTitle, answer));
            }
        }

        var comments =
                stackoverflowClient.getComments(questionId, "desc", "creation", "stackoverflow", FILTER_WITH_BODY);
        for (var comment : comments.items()) {
            if (comment.creationDate().isAfter(lastCheckedAt)) {
                updates.add(formatter.formatComment(questionTitle, comment));
            }
        }

        return updates;
    }
}
