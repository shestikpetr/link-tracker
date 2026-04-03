package backend.academy.linktracker.scrapper.service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkChecker {
    private final List<LinkHandler> linkHandlers;

    public boolean supports(URI url) {
        return linkHandlers.stream().anyMatch(h -> h.supports(url));
    }

    public Optional<Instant> getLastActivity(URI url) {
        return linkHandlers.stream().filter(h -> h.supports(url)).findFirst().map(h -> h.getLastActivity(url));
    }
}
