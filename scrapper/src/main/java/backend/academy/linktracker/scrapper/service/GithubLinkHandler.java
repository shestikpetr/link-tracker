package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.GithubClient;
import java.net.URI;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GithubLinkHandler implements LinkHandler {
    private final GithubClient githubClient;

    @Override
    public boolean supports(URI url) {
        return "github.com".equals(url.getHost());
    }

    @Override
    public Instant getLastActivity(URI url) {
        String[] parts = url.getPath().split("/");
        return githubClient.getRepository(parts[1], parts[2]).pushedAt();
    }
}
