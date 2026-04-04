package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.dto.GithubIssueResponse;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkHandler implements LinkHandler {
    private static final int PREVIEW_MAX_LENGTH = 200;
    private static final int PER_PAGE = 30;
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneOffset.UTC);

    private final GithubClient githubClient;

    @Override
    public boolean supports(URI url) {
        if (!"github.com".equals(url.getHost())) return false;
        String[] parts = url.getPath().split("/");
        return parts.length >= 3 && !parts[1].isBlank() && !parts[2].isBlank();
    }

    @Override
    public List<LinkUpdateInfo> checkUpdates(URI url, Instant lastCheckedAt) {
        String[] parts = url.getPath().split("/");
        String owner = parts[1];
        String repo = parts[2];

        String since = DateTimeFormatter.ISO_INSTANT.format(lastCheckedAt);
        List<GithubIssueResponse> issues =
                githubClient.getIssues(owner, repo, "all", "created", "desc", since, PER_PAGE);

        return issues.stream()
                .filter(issue -> issue.createdAt().isAfter(lastCheckedAt))
                .map(this::toUpdateInfo)
                .toList();
    }

    private LinkUpdateInfo toUpdateInfo(GithubIssueResponse issue) {
        String type = issue.isPullRequest() ? "Pull Request" : "Issue";
        String userName = issue.user() != null ? issue.user().login() : "unknown";
        String preview = truncate(issue.body());
        String time = DISPLAY_FORMAT.format(issue.createdAt());

        String description = """
            Новый %s
            Название: %s
            Автор: %s
            Время: %s
            Превью: %s\
            """.formatted(type, issue.title(), userName, time, preview);

        return new LinkUpdateInfo(issue.createdAt(), description);
    }

    private String truncate(String text) {
        if (text == null || text.isBlank()) {
            return "(нет описания)";
        }
        return text.length() <= PREVIEW_MAX_LENGTH ? text : text.substring(0, PREVIEW_MAX_LENGTH) + "...";
    }
}
