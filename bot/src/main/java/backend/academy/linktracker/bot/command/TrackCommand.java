package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_TAGS;
import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_URL;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.state.ChatState;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class TrackCommand implements Command, StatefulCommand {
    private final ScrapperClient scrapperClient;
    private final ChatStateService chatStateService;

    public TrackCommand(ScrapperClient scrapperClient, ChatStateService chatStateService) {
        this.scrapperClient = scrapperClient;
        this.chatStateService = chatStateService;
    }

    @Override
    public String command() {
        return "/track";
    }

    @Override
    public String description() {
        return "Начать отслеживание ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();

        chatStateService.setState(chatId, WAITING_TRACK_URL);

        return new SendMessage(chatId, "Введите URL:");
    }

    @Override
    public Set<ChatState> handledStates() {
        return Set.of(WAITING_TRACK_URL, WAITING_TRACK_TAGS);
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        ChatState state = chatStateService.getState(chatId).orElseThrow();
        String text;

        switch (state) {
            case WAITING_TRACK_URL -> {
                chatStateService.setPendingUrl(
                        chatId, URI.create(update.message().text().trim()));
                chatStateService.setState(chatId, WAITING_TRACK_TAGS);
                text = "Введите теги:";
            }
            case WAITING_TRACK_TAGS -> {
                URI url = chatStateService.getPendingUrl(chatId);
                List<String> tags = Arrays.stream(update.message().text().split(","))
                        .map(String::trim)
                        .filter(t -> !t.isEmpty())
                        .toList();

                scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));

                chatStateService.clearState(chatId);

                text = "Ссылка добавлена.";
            }
            default -> throw new IllegalStateException("Произошла ошибка: " + state);
        }

        return new SendMessage(chatId, text);
    }
}
