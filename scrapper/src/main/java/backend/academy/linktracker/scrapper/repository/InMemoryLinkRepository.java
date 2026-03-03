package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository implements LinkRepository {
    private final Map<Long, Map<Long, TrackedLink>> storage = new ConcurrentHashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public void registerChat(Long chatId) {
        if (storage.containsKey(chatId)) throw new ChatAlreadyExistsException(chatId);
        storage.put(chatId, new ConcurrentHashMap<>());
    }

    @Override
    public void deleteChat(Long chatId) {
        if (storage.remove(chatId) == null) throw new ChatNotFoundException(chatId);
    }

    @Override
    public LinkResponse addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        Map<Long, TrackedLink> links = getChat(chatId);
        boolean exists = links.values().stream().anyMatch(l -> l.url().equals(url));
        if (exists) throw new LinkAlreadyExistsException(url);

        long id = idGen.getAndIncrement();
        TrackedLink link = new TrackedLink(id, url, tags, filters, Instant.now());
        links.put(id, link);
        return trackedLinkToLinkResponse(link);
    }

    @Override
    public LinkResponse removeLink(Long chatId, URI url) {
        Map<Long, TrackedLink> links = getChat(chatId);
        return links.values().stream()
                .filter(l -> l.url().equals(url))
                .findFirst()
                .map(l -> {
                    links.remove(l.id());
                    return trackedLinkToLinkResponse(l);
                })
                .orElseThrow(() -> new LinkNotFoundException(url));
    }

    @Override
    public List<LinkResponse> findByChat(Long chatId) {
        return getChat(chatId).values().stream()
                .map(this::trackedLinkToLinkResponse)
                .toList();
    }

    @Override
    public Map<Long, List<TrackedLink>> findAllGroupedByChat() {
        return storage.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, e -> List.copyOf(e.getValue().values())));
    }

    private Map<Long, TrackedLink> getChat(Long chatId) {
        Map<Long, TrackedLink> links = storage.get(chatId);
        if (links == null) throw new ChatNotFoundException(chatId);
        return links;
    }

    private LinkResponse trackedLinkToLinkResponse(TrackedLink trackedLink) {
        return new LinkResponse(trackedLink.id(), trackedLink.url(), trackedLink.tags(), trackedLink.filters());
    }
}
