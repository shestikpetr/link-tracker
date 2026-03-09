package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_TAGS;

import backend.academy.linktracker.bot.exceptions.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.bot.service.LinkTrackingService;
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
public class TrackTagsStateHandler implements StatefulCommand {
    private final LinkTrackingService linkTrackingService;
    private final ChatStateService chatStateService;
    private final TagParser tagParser;

    @Override
    public Set<ChatState> handledStates() {
        return Set.of(WAITING_TRACK_TAGS);
    }

    @Override
    public SendMessage handleInput(Update update) {
        long chatId = update.message().chat().id();
        URI url = chatStateService.getPendingUrl(chatId);
        List<String> tags = tagParser.parseTags(update.message().text());
        chatStateService.clearState(chatId);

        String text;
        try {
            linkTrackingService.addLink(chatId, url, tags);
            text = "Ссылка добавлена.";
        } catch (LinkAlreadyTrackedException | UnsupportedLinkException e) {
            text = e.getMessage();
        }

        return new SendMessage(chatId, text);
    }
}
