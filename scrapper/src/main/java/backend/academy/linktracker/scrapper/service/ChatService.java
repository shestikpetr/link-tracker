package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatRepository chatRepository;
    private final LinkRepository linkRepository;

    public void register(Long chatId) {
        if (!chatRepository.registerChat(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }
    }

    @Transactional
    public void delete(Long chatId) {
        if (!chatRepository.chatExists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        linkRepository.deleteByChat(chatId);
        chatRepository.deleteChat(chatId);
    }
}
