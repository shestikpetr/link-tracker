package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
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
    public boolean existsChat(Long chatId) {
        return chats.contains(chatId);
    }
}
