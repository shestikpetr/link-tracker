package backend.academy.linktracker.scrapper.service.formatter;

import backend.academy.linktracker.scrapper.dto.GithubIssueResponse;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.utils.TextUtils;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubUpdateFormatter {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final TextUtils textUtils;

    public LinkUpdateInfo format(GithubIssueResponse issue) {
        String type = issue.isPullRequest() ? "Pull Request" : "Issue";
        String userName = issue.user() != null ? issue.user().login() : "unknown";
        String preview = textUtils.truncate(issue.body(), "(нет описания)");
        String time = DISPLAY_FORMAT.format(issue.createdAt());

        String description = String.join(
                "\n",
                "Новый " + type,
                "Название: " + issue.title(),
                "Автор: " + userName,
                "Время: " + time,
                "Превью: " + preview);

        return new LinkUpdateInfo(issue.createdAt(), description);
    }
}
