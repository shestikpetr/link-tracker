package backend.academy.linktracker.bot.state;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ChatStateService {
    private final Map<Long, ChatState> states = new ConcurrentHashMap<>();
    private final Map<Long, URI> pendingUrls = new ConcurrentHashMap<>();

    public void setState(Long chatId, ChatState state) {
        states.put(chatId, state);
    }

    public void clearState(Long chatId) {
        states.remove(chatId);
        pendingUrls.remove(chatId);
    }

    public Optional<ChatState> getState(Long chatId) {
        return Optional.ofNullable(states.get(chatId));
    }

    public void setPendingUrl(Long chatId, URI url) {
        pendingUrls.put(chatId, url);
    }

    public URI getPendingUrl(Long chatId) {
        return pendingUrls.get(chatId);
    }
}
