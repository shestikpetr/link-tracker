package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import java.net.URI;
import java.util.List;
import java.util.Map;

public interface LinkRepository {
    void registerChat(Long chatId);

    void deleteChat(Long chatId);

    LinkResponse addLink(Long chatId, URI url, List<String> tags, List<String> filters);

    LinkResponse removeLink(Long chatId, URI url);

    List<LinkResponse> findByChat(Long chatId);

    Map<Long, List<LinkResponse>> findAllGroupedByChat();
}
