package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.client.ScrapperClient;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class ListCommand implements Command {
    private final ScrapperClient scrapperClient;

    public ListCommand(ScrapperClient scrapperClient) {
        this.scrapperClient = scrapperClient;
    }

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

        var links = scrapperClient.getLinks(chatId).links();
        String text;

        if (!links.isEmpty()) {
            text = links.stream().map(link -> link.url().toString()).collect(Collectors.joining("\n"));
        } else {
            text = "Нет отслеживаемых ссылок.";
        }

        return new SendMessage(chatId, text);
    }
}
