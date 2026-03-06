package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import java.net.URI;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class StackoverflowLinkHandler implements LinkHandler {
    private final StackoverflowClient stackoverflowClient;

    public StackoverflowLinkHandler(StackoverflowClient stackoverflowClient) {
        this.stackoverflowClient = stackoverflowClient;
    }

    @Override
    public boolean supports(URI url) {
        return "stackoverflow.com".equals(url.getHost());
    }

    @Override
    public Instant getLastActivity(URI url) {
        String[] parts = url.getPath().split("/");
        return stackoverflowClient
                .getQuestions(Long.parseLong(parts[2]))
                .items()
                .getFirst()
                .lastActivityDate();
    }
}
