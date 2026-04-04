package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.dto.StackoverflowAnswerResponse;
import backend.academy.linktracker.scrapper.dto.StackoverflowCommentResponse;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackoverflowLinkHandler implements LinkHandler {
    private static final int PREVIEW_MAX_LENGTH = 200;
    private static final String FILTER_WITH_BODY = "withbody";
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneOffset.UTC);

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
    public List<LinkUpdateInfo> checkUpdates(URI url, Instant lastCheckedAt) {
        Long questionId = extractQuestionId(url);

        String questionTitle = fetchQuestionTitle(questionId);
        List<LinkUpdateInfo> updates = new ArrayList<>();

        var answers = stackoverflowClient.getAnswers(questionId, "desc", "creation", "stackoverflow", FILTER_WITH_BODY);
        for (StackoverflowAnswerResponse.Item answer : answers.items()) {
            if (answer.creationDate().isAfter(lastCheckedAt)) {
                updates.add(toAnswerUpdateInfo(questionTitle, answer));
            }
        }

        var comments =
                stackoverflowClient.getComments(questionId, "desc", "creation", "stackoverflow", FILTER_WITH_BODY);
        for (StackoverflowCommentResponse.Item comment : comments.items()) {
            if (comment.creationDate().isAfter(lastCheckedAt)) {
                updates.add(toCommentUpdateInfo(questionTitle, comment));
            }
        }

        return updates;
    }

    private Long extractQuestionId(URI url) {
        String[] parts = url.getPath().split("/");
        return Long.parseLong(parts[2]);
    }

    private String fetchQuestionTitle(Long questionId) {
        var items = stackoverflowClient.getQuestions(questionId).items();
        if (items.isEmpty()) {
            return "(неизвестный вопрос)";
        }
        return items.getFirst().title();
    }

    private LinkUpdateInfo toAnswerUpdateInfo(String questionTitle, StackoverflowAnswerResponse.Item answer) {
        String userName = answer.owner() != null ? answer.owner().displayName() : "unknown";
        String preview = truncate(answer.body());
        String time = DISPLAY_FORMAT.format(answer.creationDate());

        String description = """
            Новый ответ
            Вопрос: %s
            Автор: %s
            Время: %s
            Превью: %s\
            """.formatted(questionTitle, userName, time, preview);

        return new LinkUpdateInfo(answer.creationDate(), description);
    }

    private LinkUpdateInfo toCommentUpdateInfo(String questionTitle, StackoverflowCommentResponse.Item comment) {
        String userName = comment.owner() != null ? comment.owner().displayName() : "unknown";
        String preview = truncate(comment.body());
        String time = DISPLAY_FORMAT.format(comment.creationDate());

        String description = """
            Новый комментарий
            Вопрос: %s
            Автор: %s
            Время: %s
            Превью: %s\
            """.formatted(questionTitle, userName, time, preview);

        return new LinkUpdateInfo(comment.creationDate(), description);
    }

    private String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "(нет текста)";
        }
        return text.length() <= PREVIEW_MAX_LENGTH ? text : text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
}
