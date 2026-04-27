package backend.academy.linktracker.scrapper.repository.inmemory;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "INMEMORY")
public class InMemoryLinkRepository implements LinkRepository {
    private final Map<Long, Map<Long, TrackedLink>> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public Optional<TrackedLink> addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        Map<Long, TrackedLink> links = getOrCreateChatLinks(chatId);
        boolean exists = links.values().stream().anyMatch(l -> l.url().equals(url));

        if (exists) {
            return Optional.empty();
        }

        long id = idGen.getAndIncrement();
        TrackedLink link = new TrackedLink(id, url, tags, filters, Instant.now());
        links.put(id, link);

        return Optional.of(link);
    }

    @Override
    public Optional<TrackedLink> updateLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        Map<Long, TrackedLink> links = getChatLinks(chatId);

        return links.values().stream()
                .filter(l -> l.url().equals(url))
                .findFirst()
                .map(l -> {
                    TrackedLink updated = new TrackedLink(l.id(), l.url(), tags, filters, l.lastCheckedAt());
                    links.put(l.id(), updated);
                    return updated;
                });
    }

    @Override
    public Optional<TrackedLink> removeLink(Long chatId, URI url) {
        Map<Long, TrackedLink> links = getChatLinks(chatId);

        return links.values().stream()
                .filter(l -> l.url().equals(url))
                .findFirst()
                .map(l -> {
                    links.remove(l.id());
                    return l;
                });
    }

    @Override
    public Collection<TrackedLink> findByChat(Long chatId) {
        return getChatLinks(chatId).values();
    }

    @Override
    public Collection<TrackedLink> findByChatAndTags(Long chatId, List<String> tags) {
        return getChatLinks(chatId).values().stream()
                .filter(link -> link.tags().stream().anyMatch(tags::contains))
                .toList();
    }

    @Override
    public Map<URI, List<ChatLink>> findAllGroupedByUrl() {
        Map<URI, List<ChatLink>> result = new LinkedHashMap<>();

        for (var chatEntry : storage.entrySet()) {
            Long chatId = chatEntry.getKey();

            for (TrackedLink link : chatEntry.getValue().values()) {
                result.computeIfAbsent(link.url(), _ -> new ArrayList<>()).add(new ChatLink(chatId, link));
            }
        }

        return result;
    }

    @Override
    public void deleteByChat(Long chatId) {
        storage.remove(chatId);
    }

    @Override
    public void updateLastChecked(Long linkId, Instant lastCheckedAt) {
        storage.values()
                .forEach(links -> links.computeIfPresent(
                        linkId,
                        (_, link) ->
                                new TrackedLink(link.id(), link.url(), link.tags(), link.filters(), lastCheckedAt)));
    }

    @Override
    public Map<URI, List<ChatLink>> findStaleLinksGroupedByUrl(int limit) {
        Map<URI, List<ChatLink>> all = findAllGroupedByUrl();

        return all.entrySet().stream()
                .sorted((a, b) -> {
                    Instant aMin = a.getValue().stream()
                            .map(cl -> cl.link().lastCheckedAt())
                            .min(Instant::compareTo)
                            .orElse(Instant.MAX);
                    Instant bMin = b.getValue().stream()
                            .map(cl -> cl.link().lastCheckedAt())
                            .min(Instant::compareTo)
                            .orElse(Instant.MAX);
                    return aMin.compareTo(bMin);
                })
                .limit(limit)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
    }

    private Map<Long, TrackedLink> getOrCreateChatLinks(Long chatId) {
        return storage.computeIfAbsent(chatId, _ -> new ConcurrentHashMap<>());
    }

    private Map<Long, TrackedLink> getChatLinks(Long chatId) {
        Map<Long, TrackedLink> links = storage.get(chatId);
        return links != null ? links : Map.of();
    }
}
