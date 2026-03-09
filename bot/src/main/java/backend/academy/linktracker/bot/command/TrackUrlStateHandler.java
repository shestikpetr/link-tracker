package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_TAGS;
import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_URL;

import backend.academy.linktracker.bot.state.ChatState;
import backend.academy.linktracker.bot.state.ChatStateService;
import backend.academy.linktracker.bot.utils.UrlValidator;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackUrlStateHandler implements StatefulCommand {
    private final ChatStateService chatStateService;
    private final UrlValidator urlValidator;

    @Override
    public Set<ChatState> handledStates() {
        return Set.of(WAITING_TRACK_URL);
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        String text = update.message().text().trim();

        if (!urlValidator.isValid(text)) {
            return new SendMessage(chatId, "Некорректная ссылка. Введите ссылку ещё раз:");
        }

        chatStateService.setPendingUrl(chatId, urlValidator.parse(text));
        chatStateService.setState(chatId, WAITING_TRACK_TAGS);
        return new SendMessage(chatId, "Введите теги:");
    }
}
