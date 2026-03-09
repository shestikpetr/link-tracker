package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface LinkRepository {
    void registerChat(Long chatId);

    void deleteChat(Long chatId);

    TrackedLink addLink(Long chatId, URI url, List<String> tags, List<String> filters);

    TrackedLink removeLink(Long chatId, URI url);

    List<TrackedLink> findByChat(Long chatId);

    Map<Long, List<TrackedLink>> findAllGroupedByChat();
}
