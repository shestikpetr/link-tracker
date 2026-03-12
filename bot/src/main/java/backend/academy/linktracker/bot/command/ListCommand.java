package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.utils.TagParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class ListCommand implements Command {
    private final LinkTrackingService linkTrackingService;
    private final TagParser tagParser;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Вывести список всех отслеживаемых ссылок";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        String[] parts = update.message().text().trim().split("\\s+", 2);
        List<String> tags = parts.length > 1 ? tagParser.parseTags(parts[1]) : null;

        List<LinkResponse> links = linkTrackingService.getLinks(chatId, tags);
        String text = links.isEmpty()
                ? "Нет отслеживаемых ссылок."
                : links.stream().map(link -> link.url().toString()).collect(Collectors.joining("\n"));
        return new SendMessage(chatId, text);
    }
}
