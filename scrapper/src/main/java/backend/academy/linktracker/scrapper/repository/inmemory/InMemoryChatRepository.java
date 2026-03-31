package backend.academy.linktracker.scrapper.repository.inmemory;

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
    public boolean registerChat(Long chatId) {
        return chats.add(chatId);
    }

    @Override
    public boolean deleteChat(Long chatId) {
        return chats.remove(chatId);
    }

    @Override
    public boolean chatExists(Long chatId) {
        return chats.contains(chatId);
    }
}
