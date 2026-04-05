package backend.academy.linktracker.scrapper.service.formatter;

import backend.academy.linktracker.scrapper.dto.StackoverflowAnswerResponse;
import backend.academy.linktracker.scrapper.dto.StackoverflowCommentResponse;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.utils.TextUtils;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StackoverflowUpdateFormatter {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final TextUtils textUtils;

    public LinkUpdateInfo formatAnswer(String questionTitle, StackoverflowAnswerResponse.Item answer) {
        String userName = answer.owner() != null ? answer.owner().displayName() : "unknown";
        String preview = textUtils.truncate(answer.body(), "(нет текста)");
        String time = DISPLAY_FORMAT.format(answer.creationDate());

        String description = String.join(
                "\n",
                "Новый ответ",
                "Вопрос: " + questionTitle,
                "Автор: " + userName,
                "Время: " + time,
                "Превью: " + preview);

        return new LinkUpdateInfo(answer.creationDate(), description);
    }

    public LinkUpdateInfo formatComment(String questionTitle, StackoverflowCommentResponse.Item comment) {
        String userName = comment.owner() != null ? comment.owner().displayName() : "unknown";
        String preview = textUtils.truncate(comment.body(), "(нет текста)");
        String time = DISPLAY_FORMAT.format(comment.creationDate());

        String description = String.join(
                "\n",
                "Новый комментарий",
                "Вопрос: " + questionTitle,
                "Автор: " + userName,
                "Время: " + time,
                "Превью: " + preview);

        return new LinkUpdateInfo(comment.creationDate(), description);
    }
}
