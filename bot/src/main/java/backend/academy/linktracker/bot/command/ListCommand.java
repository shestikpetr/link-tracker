package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.dto.LinkResponse;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.utils.ArgumentParser;
import backend.academy.linktracker.bot.utils.TagParser;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListCommand implements Command {
    private final LinkTrackingService linkTrackingService;
    private final TagParser tagParser;
    private final ArgumentParser argumentParser;

    @Override
    public int order() {
        return 2;
    }

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
        List<String> tags = argumentParser
                .parseArguments(update.message().text())
                .map(tagParser::parseTags)
                .orElse(null);

        List<LinkResponse> links = linkTrackingService.getLinks(chatId, tags);
        String text = links.isEmpty()
                ? "Нет отслеживаемых ссылок."
                : links.stream().map(link -> link.url().toString()).collect(Collectors.joining("\n"));
        return new SendMessage(chatId, text);
    }
}
