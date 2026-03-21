package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatEntity;
import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
public class OrmChatRepository implements ChatRepository {
    private final JpaChatRepository jpaChatRepository;

    @Override
    public void registerChat(Long chatId) {
        if (jpaChatRepository.existsById(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }

        ChatEntity chat = new ChatEntity();
        chat.setId(chatId);
        jpaChatRepository.save(chat);
    }

    @Override
    public void deleteChat(Long chatId) {
        if (!jpaChatRepository.existsById(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        jpaChatRepository.deleteById(chatId);
    }

    @Override
    public boolean chatExists(Long chatId) {
        return jpaChatRepository.existsById(chatId);
    }
}
