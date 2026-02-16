package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HelpCommand implements Command {
    private final List<Command> commands;

    public HelpCommand(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Вывести все доступные команды пользователю";
    }

    @Override
    public SendMessage handle(Update update) {
        String text = commands.stream()
                .map(command -> command.command() + " - " + command.description())
                .collect(Collectors.joining("\n"));

        long chatId = update.message().chat().id();
        return new SendMessage(chatId, text);
    }
}
