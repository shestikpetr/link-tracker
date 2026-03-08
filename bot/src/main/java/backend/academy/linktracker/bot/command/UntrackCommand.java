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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class UntrackCommand implements Command, StatefulCommand {

    private final ScrapperClient scrapperClient;
    private final ChatStateService chatStateService;

    public UntrackCommand(ScrapperClient scrapperClient, ChatStateService chatStateService) {
        this.scrapperClient = scrapperClient;
        this.chatStateService = chatStateService;
    }

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Прекратить отслеживание ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();

        chatStateService.setState(chatId, WAITING_UNTRACK_URL);

        return new SendMessage(chatId, "Введите URL:");
    }

    @Override
    public Set<ChatState> handledStates() {
        return Set.of(WAITING_UNTRACK_URL);
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        ChatState state = chatStateService.getState(chatId).orElseThrow();
        String text;

        if (state == WAITING_UNTRACK_URL) {
            scrapperClient.removeLink(
                    chatId,
                    new RemoveLinkRequest(URI.create(update.message().text().trim())));

            text = "Ссылка удалена.";

            chatStateService.clearState(chatId);
        } else {
            throw new IllegalStateException("Произошла ошибка: " + state);
        }

        return new SendMessage(chatId, text);
    }
}
