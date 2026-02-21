package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exception.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exception.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exception.LinkNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class InMemoryLinkRepository implements LinkRepository {
    private final Map<Long, Map<Long, LinkResponse>> storage = new ConcurrentHashMap<>();
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
        Map<Long, LinkResponse> links = getChat(chatId);
        boolean exists = links.values().stream().anyMatch(l -> l.url().equals(url));
        if (exists) throw new LinkAlreadyExistsException(url);

        long id = idGen.getAndIncrement();
        LinkResponse link = new LinkResponse(id, url, tags, filters);
        links.put(id, link);
        return link;
    }

    @Override
    public LinkResponse removeLink(Long chatId, URI url) {
        Map<Long, LinkResponse> links = getChat(chatId);
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
    public List<LinkResponse> findByChat(Long chatId) {
        return List.copyOf(getChat(chatId).values());
    }

    @Override
    public Map<Long, List<LinkResponse>> findAllGroupedByChat() {
        return storage.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, e -> List.copyOf(e.getValue().values())));
    }

    private Map<Long, LinkResponse> getChat(Long chatId) {
        Map<Long, LinkResponse> links = storage.get(chatId);
        if (links == null) throw new ChatNotFoundException(chatId);
        return links;
    }
}
