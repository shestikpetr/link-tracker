package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_TAGS;
import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_URL;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.exceptions.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exceptions.LinkNotFoundException;
import backend.academy.linktracker.bot.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.bot.state.ChatState;
import backend.academy.linktracker.bot.state.ChatStateService;
import backend.academy.linktracker.bot.utils.TagParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackStateHandler implements StatefulCommand {
    private final ScrapperClient scrapperClient;
    private final ChatStateService chatStateService;
    private final TagParser tagParser;

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
                try {
                    URI uri = URI.create(update.message().text().trim());
                    if (!uri.isAbsolute()
                            || !(uri.getScheme().equals("http")
                                    || uri.getScheme().equals("https"))) {
                        return new SendMessage(chatId, "Некорректная ссылка. Введите ссылку ещё раз:");
                    }
                    chatStateService.setPendingUrl(chatId, uri);
                } catch (IllegalArgumentException e) {
                    return new SendMessage(chatId, "Некорректная ссылка. Введите ссылку ещё раз:");
                }
                chatStateService.setState(chatId, WAITING_TRACK_TAGS);
                text = "Введите теги:";
            }
            case WAITING_TRACK_TAGS -> {
                URI url = chatStateService.getPendingUrl(chatId);
                List<String> tags = tagParser.parseTags(update.message().text());
                try {
                    scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));
                    text = "Ссылка добавлена.";
                } catch (LinkAlreadyTrackedException | UnsupportedLinkException e) {
                    text = e.getMessage();
                } catch (LinkNotFoundException e) {
                    scrapperClient.registerChat(chatId);
                    scrapperClient.addLink(chatId, new AddLinkRequest(url, tags, List.of()));
                    text = "Ссылка добавлена.";
                }
                chatStateService.clearState(chatId);
            }
            default -> throw new IllegalStateException("Произошла ошибка: " + state);
        }

        return new SendMessage(chatId, text);
    }
}
