package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.ChatSession;
import backend.academy.linktracker.bot.state.ChatStateService;
import backend.academy.linktracker.bot.utils.UrlValidator;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackUrlStateHandler implements StateHandler {
    private final ChatStateService chatStateService;
    private final UrlValidator urlValidator;

    @Override
    public Class<? extends ChatSession> handledSessionType() {
        return ChatSession.TrackUrl.class;
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        Optional<URI> url = urlValidator.parse(update.message().text());

        if (url.isEmpty()) {
            return new SendMessage(chatId, "Некорректная ссылка. Введите ссылку ещё раз:");
        }

        chatStateService.setSession(chatId, new ChatSession.TrackTags(url.orElseThrow()));
        return new SendMessage(chatId, "Введите теги:");
    }
}
