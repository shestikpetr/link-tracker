package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
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

    public List<LinkResponse> getLinks(Long chatId, List<String> tags) {
        requireChatExists(chatId);
        return linkRepository.findByChat(chatId).stream()
                .filter(link ->
                        tags == null || tags.isEmpty() || link.tags().stream().anyMatch(tags::contains))
                .map(this::toResponse)
                .toList();
    }

    public LinkResponse addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        requireChatExists(chatId);
        if (!linkChecker.supports(url)) {
            throw new UnsupportedLinkException(url);
        }
        return linkRepository
                .addLink(chatId, url, tags, filters)
                .map(this::toResponse)
                .orElseThrow(() -> new LinkAlreadyExistsException(url));
    }

    public LinkResponse removeLink(Long chatId, URI url) {
        requireChatExists(chatId);
        return linkRepository
                .removeLink(chatId, url)
                .map(this::toResponse)
                .orElseThrow(() -> new LinkNotFoundException(url));
    }

    private void requireChatExists(Long chatId) {
        if (!chatRepository.chatExists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
    }

    private LinkResponse toResponse(TrackedLink link) {
        return new LinkResponse(link.id(), link.url(), link.tags(), link.filters());
    }
}
