package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkChecker {
    private final List<LinkHandler> linkHandlers;

    public Optional<Instant> checkLink(TrackedLink link) {
        return linkHandlers.stream()
                .filter(h -> h.supports(link.url()))
                .findFirst()
                .map(h -> h.getLastActivity(link.url()))
                .filter(lastActivity -> lastActivity.isAfter(link.lastCheckedAt()));
    }
}
