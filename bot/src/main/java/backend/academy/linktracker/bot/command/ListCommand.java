package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.utils.TagParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@RequiredArgsConstructor
public class ListCommand implements Command {
    private final ScrapperClient scrapperClient;
    private final TagParser tagParser;

    @Override
    public String command() {
        return "/list";
    }

    @Override
    public String description() {
        return "Вывести список всех отслеживаемы ссылок";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        String[] parts = update.message().text().trim().split("\\s+", 2);
        String tag = parts.length > 1 ? tagParser.normalize(parts[1]) : null;

        var links = scrapperClient.getLinks(chatId, tag);
        String text;

        if (!links.isEmpty()) {
            text = links.stream().map(link -> link.url().toString()).collect(Collectors.joining("\n"));
        } else {
            text = "Нет отслеживаемых ссылок.";
        }

        return new SendMessage(chatId, text);
    }
}
