package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface LinkRepository {
    Optional<TrackedLink> addLink(Long chatId, URI url, List<String> tags, List<String> filters);

    Optional<TrackedLink> updateLink(Long chatId, URI url, List<String> tags, List<String> filters);

    Optional<TrackedLink> removeLink(Long chatId, URI url);

    Collection<TrackedLink> findByChat(Long chatId);

    Collection<TrackedLink> findByChatAndTags(Long chatId, List<String> tags);

    Map<URI, List<ChatLink>> findAllGroupedByUrl();

    void deleteByChat(Long chatId);

    void updateLastChecked(Long linkId, Instant lastCheckedAt);

    Map<URI, List<ChatLink>> findStaleLinksGroupedByUrl(int limit);
}
