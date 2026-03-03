package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkService {
    private final LinkRepository linkRepository;

    public void registerChat(Long chatId) {
        linkRepository.registerChat(chatId);
    }

    public void deleteChat(Long chatId) {
        linkRepository.deleteChat(chatId);
    }

    public ListLinksResponse getLinks(Long chatId) {
        List<LinkResponse> links = linkRepository.findByChat(chatId);
        return new ListLinksResponse(links, links.size());
    }

    public LinkResponse addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        return linkRepository.addLink(chatId, url, tags, filters);
    }

    public LinkResponse removeLink(Long chatId, URI url) {
        return linkRepository.removeLink(chatId, url);
    }

    public Map<Long, List<TrackedLink>> findAllGroupedByChat() {
        return linkRepository.findAllGroupedByChat();
    }
}
