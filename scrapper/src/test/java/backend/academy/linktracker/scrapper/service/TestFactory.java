package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import java.util.List;

final class TestFactory {

    private TestFactory() {}

    static TrackedLink trackedLink(Long id, URI url, int secondsAgo) {
        return new TrackedLink(id, url, List.of(), List.of(), Instant.now().minusSeconds(secondsAgo));
    }
}
