package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatEntity;
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
    public boolean registerChat(Long chatId) {
        if (jpaChatRepository.existsById(chatId)) {
            return false;
        }

        ChatEntity chat = new ChatEntity();
        chat.setId(chatId);
        jpaChatRepository.save(chat);
        return true;
    }

    @Override
    public boolean deleteChat(Long chatId) {
        if (!jpaChatRepository.existsById(chatId)) {
            return false;
        }

        jpaChatRepository.deleteById(chatId);
        return true;
    }

    @Override
    public boolean chatExists(Long chatId) {
        return jpaChatRepository.existsById(chatId);
    }
}
