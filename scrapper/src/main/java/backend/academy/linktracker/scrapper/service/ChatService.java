package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    public void register(Long chatId) {
        chatRepository.registerChat(chatId);
    }

    public void delete(Long chatId) {
        chatRepository.deleteChat(chatId);
        linkRepository.deleteByChat(chatId);
    }
}
