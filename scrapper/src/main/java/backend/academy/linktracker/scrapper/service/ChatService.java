package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final LinkRepository linkRepository;

    public void register(Long chatId) {
        linkRepository.registerChat(chatId);
    }

    public void delete(Long chatId) {
        linkRepository.deleteChat(chatId);
    }
}
