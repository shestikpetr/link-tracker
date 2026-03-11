package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {
    private final LinkRepository linkRepository;

    public void register(Long chatId) {
        log.info("Регистрация чата {}", chatId);
        linkRepository.registerChat(chatId);
    }

    public void delete(Long chatId) {
        log.info("Удаление чата {}", chatId);
        linkRepository.deleteChat(chatId);
    }
}
