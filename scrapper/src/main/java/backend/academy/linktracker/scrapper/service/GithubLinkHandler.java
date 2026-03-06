package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.GithubClient;
import java.net.URI;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class GithubLinkHandler implements LinkHandler {
    private final GithubClient githubClient;

    public GithubLinkHandler(GithubClient githubClient) {
        this.githubClient = githubClient;
    }

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
