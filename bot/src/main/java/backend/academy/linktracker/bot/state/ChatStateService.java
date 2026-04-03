package backend.academy.linktracker.bot.state;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ChatStateService {
    private final Map<Long, ChatSession> sessions = new ConcurrentHashMap<>();

    public void setSession(Long chatId, ChatSession session) {
        sessions.put(chatId, session);
    }

    public void clearSession(Long chatId) {
        sessions.remove(chatId);
    }

    public Optional<ChatSession> getSession(Long chatId) {
        return Optional.ofNullable(sessions.get(chatId));
    }
}
