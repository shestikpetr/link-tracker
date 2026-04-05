package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.service.formatter.GithubUpdateFormatter;
import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkHandler implements LinkHandler {
    private static final int PER_PAGE = 30;

    private final GithubClient githubClient;
    private final GithubUpdateFormatter formatter;
    private final GithubLinkExtractor extractor;

    @Override
    public boolean supports(URI url) {
        return extractor.supports(url);
    }

    @Override
    public List<LinkUpdateInfo> checkUpdates(URI url, Instant lastCheckedAt) {
        String owner = extractor.extractOwner(url);
        String repo = extractor.extractRepo(url);

        String since = DateTimeFormatter.ISO_INSTANT.format(lastCheckedAt);
        var issues = githubClient.getIssues(owner, repo, "all", "created", "desc", since, PER_PAGE);

        return issues.stream()
                .filter(issue -> issue.createdAt().isAfter(lastCheckedAt))
                .map(formatter::format)
                .toList();
    }
}
