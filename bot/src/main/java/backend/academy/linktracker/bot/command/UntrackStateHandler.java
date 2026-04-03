package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.exceptions.LinkNotFoundException;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.state.ChatSession;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UntrackStateHandler implements StateHandler {
    private final LinkTrackingService linkTrackingService;
    private final ChatStateService chatStateService;

    @Override
    public Class<? extends ChatSession> handledSessionType() {
        return ChatSession.Untrack.class;
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        String text;

        try {
            linkTrackingService.removeLink(
                    chatId, URI.create(update.message().text().trim()));
            chatStateService.clearSession(chatId);
            text = "Ссылка удалена.";
        } catch (LinkNotFoundException e) {
            text = e.getMessage();
        }

        return new SendMessage(chatId, text);
    }
}
