package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkCacheService {
    private final LinkRepository linkRepository;

    @Cacheable(cacheNames = "links", key = "#chatId")
    public List<LinkResponse> getByChat(Long chatId) {
        return linkRepository.findByChat(chatId).stream()
                .map(LinkResponse::from)
                .toList();
    }

    @CacheEvict(cacheNames = "links", key = "#chatId")
    public void evict(Long chatId) {}
}
