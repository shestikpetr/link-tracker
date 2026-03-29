package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface LinkRepository {
    TrackedLink addLink(Long chatId, URI url, List<String> tags, List<String> filters);

    TrackedLink removeLink(Long chatId, URI url);

    Collection<TrackedLink> findByChat(Long chatId);

    Map<URI, List<ChatLink>> findAllGroupedByUrl();

    void deleteByChat(Long chatId);

    void updateLastChecked(Long linkId, Instant lastCheckedAt);
}
