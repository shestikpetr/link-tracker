package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_UNTRACK_URL;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.RemoveLinkRequest;
import backend.academy.linktracker.bot.state.ChatState;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UntrackStateHandler implements StatefulCommand {
    private final ScrapperClient scrapperClient;
    private final ChatStateService chatStateService;

    @Override
    public Set<ChatState> handledStates() {
        return Set.of(WAITING_UNTRACK_URL);
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        ChatState state = chatStateService.getState(chatId).orElseThrow();

        if (state != WAITING_UNTRACK_URL) {
            throw new IllegalStateException("Произошла ошибка: " + state);
        }

        scrapperClient.removeLink(
                chatId, new RemoveLinkRequest(URI.create(update.message().text().trim())));
        chatStateService.clearState(chatId);

        return new SendMessage(chatId, "Ссылка удалена.");
    }
}
