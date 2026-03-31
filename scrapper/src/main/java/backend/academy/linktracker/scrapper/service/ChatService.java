package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
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
        if (!chatRepository.registerChat(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }
    }

    public void delete(Long chatId) {
        if (!chatRepository.deleteChat(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
        linkRepository.deleteByChat(chatId);
    }
}
