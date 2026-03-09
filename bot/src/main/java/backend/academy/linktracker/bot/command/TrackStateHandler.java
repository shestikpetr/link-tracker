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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
@RequiredArgsConstructor
public class TrackStateHandler implements StatefulCommand {
    private final ScrapperClient scrapperClient;
    private final ChatStateService chatStateService;

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
                try {
                    scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));
                    text = "Ссылка добавлена.";
                } catch (HttpClientErrorException e) {
                    if (e.getStatusCode() == HttpStatus.CONFLICT) {
                        text = "Ссылка уже отслеживается.";
                    } else {
                        text = "Не удалось добавить ссылку.";
                    }
                }
                chatStateService.clearState(chatId);
            }
            default -> throw new IllegalStateException("Произошла ошибка: " + state);
        }

        return new SendMessage(chatId, text);
    }
}
