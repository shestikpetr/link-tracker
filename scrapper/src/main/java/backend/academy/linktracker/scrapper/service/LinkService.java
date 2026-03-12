package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository linkRepository;
    private final LinkChecker linkChecker;

    public List<LinkResponse> getLinks(Long chatId, String tag) {
        return linkRepository.findByChat(chatId).stream()
                .filter(link -> tag == null || link.tags().contains(tag))
                .map(this::toResponse)
                .toList();
    }

    public LinkResponse addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        if (!linkChecker.supports(url)) {
            throw new UnsupportedLinkException(url);
        }
        return toResponse(linkRepository.addLink(chatId, url, tags, filters));
    }

    public LinkResponse removeLink(Long chatId, URI url) {
        return toResponse(linkRepository.removeLink(chatId, url));
    }

    private LinkResponse toResponse(TrackedLink link) {
        return new LinkResponse(link.id(), link.url(), link.tags(), link.filters());
    }
}
