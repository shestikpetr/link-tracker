package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HelpCommand implements Command {
    private final CommandRegistry commandRegistry;

    @Override
    public int order() {
        return 1;
    }

    // Если мы будем использовать List<Command>, то команда Help не будет сама себя выводить из-за селф инджекта,
    // приходится получать все команды слегка по другому
    public HelpCommand(@Lazy CommandRegistry commandRegistry) {
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String command() {
        return "/help";
    }

    @Override
    public String description() {
        return "Вывод списка доступных команд";
    }

    @Override
    public SendMessage handle(Update update) {
        String text = commandRegistry.getAll().stream()
                .map(cmd -> cmd.command() + " - " + cmd.description())
                .collect(Collectors.joining("\n"));

        long chatId = update.message().chat().id();
        return new SendMessage(chatId, text);
    }
}
