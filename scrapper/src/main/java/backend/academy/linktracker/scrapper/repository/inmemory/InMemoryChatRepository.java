package backend.academy.linktracker.scrapper.repository.inmemory;

import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "INMEMORY")
public class InMemoryChatRepository implements ChatRepository {
    private final Set<Long> chats = ConcurrentHashMap.newKeySet();

    @Override
    public void registerChat(Long chatId) {
        if (!chats.add(chatId)) {
            throw new ChatAlreadyExistsException(chatId);
        }
    }

    @Override
    public void deleteChat(Long chatId) {
        if (!chats.remove(chatId)) {
            throw new ChatNotFoundException(chatId);
        }
    }

    @Override
    public boolean chatExists(Long chatId) {
        return chats.contains(chatId);
    }
}
