package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkChecker {
    private final List<LinkHandler> linkHandlers;

    public boolean supports(URI url) {
        return linkHandlers.stream().anyMatch(h -> h.supports(url));
    }

    public List<LinkUpdateInfo> checkUpdates(URI url, Instant lastCheckedAt) {
        return linkHandlers.stream()
                .filter(h -> h.supports(url))
                .findFirst()
                .map(h -> h.checkUpdates(url, lastCheckedAt))
                .orElse(List.of());
    }
}
