package backend.academy.linktracker.scrapper.repository.inmemory;

import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ChatRepository chatRepository;
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public TrackedLink addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        Map<Long, TrackedLink> links = getOrCreateChatLinks(chatId);
        boolean exists = links.values().stream().anyMatch(l -> l.url().equals(url));
        if (exists) throw new LinkAlreadyExistsException(url);

        long id = idGen.getAndIncrement();
        TrackedLink link = new TrackedLink(id, url, tags, filters, Instant.now());
        links.put(id, link);
        return link;
    }

    @Override
    public TrackedLink removeLink(Long chatId, URI url) {
        Map<Long, TrackedLink> links = getChatLinks(chatId);
        return links.values().stream()
                .filter(l -> l.url().equals(url))
                .findFirst()
                .map(l -> {
                    links.remove(l.id());
                    return l;
                })
                .orElseThrow(() -> new LinkNotFoundException(url));
    }

    @Override
    public List<TrackedLink> findByChat(Long chatId) {
        return List.copyOf(getChatLinks(chatId).values());
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

    private Map<Long, TrackedLink> getOrCreateChatLinks(Long chatId) {
        requireChatExists(chatId);
        return storage.computeIfAbsent(chatId, _ -> new ConcurrentHashMap<>());
    }

    private Map<Long, TrackedLink> getChatLinks(Long chatId) {
        requireChatExists(chatId);
        Map<Long, TrackedLink> links = storage.get(chatId);
        return links != null ? links : Map.of();
    }

    private void requireChatExists(Long chatId) {
        if (!chatRepository.existsChat(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
    }
}
