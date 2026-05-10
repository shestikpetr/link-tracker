package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository linkRepository;
    private final ChatRepository chatRepository;
    private final LinkChecker linkChecker;
    private final LinkCacheService linkCache;

    public List<LinkResponse> getLinks(Long chatId, List<String> tags) {
        requireChatExists(chatId);

        List<LinkResponse> all = linkCache.getByChat(chatId);
        if (tags == null || tags.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(link -> link.tags() != null && link.tags().stream().anyMatch(tags::contains))
                .toList();
    }

    public LinkResponse addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        requireChatExists(chatId);

        if (!linkChecker.supports(url)) {
            throw new UnsupportedLinkException(url);
        }

        LinkResponse response = linkRepository
                .addLink(chatId, url, tags, filters)
                .map(LinkResponse::from)
                .orElseThrow(() -> new LinkAlreadyExistsException(url));
        linkCache.evict(chatId);
        return response;
    }

    public LinkResponse updateLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        requireChatExists(chatId);

        LinkResponse response = linkRepository
                .updateLink(chatId, url, tags, filters)
                .map(LinkResponse::from)
                .orElseThrow(() -> new LinkNotFoundException(url));
        linkCache.evict(chatId);
        return response;
    }

    public LinkResponse removeLink(Long chatId, URI url) {
        requireChatExists(chatId);

        LinkResponse response = linkRepository
                .removeLink(chatId, url)
                .map(LinkResponse::from)
                .orElseThrow(() -> new LinkNotFoundException(url));
        linkCache.evict(chatId);
        return response;
    }

    private void requireChatExists(Long chatId) {
        if (!chatRepository.chatExists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
    }
}
