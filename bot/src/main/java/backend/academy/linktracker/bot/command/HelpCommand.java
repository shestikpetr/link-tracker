package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import java.util.stream.Collectors;

@Component
public class HelpCommand implements Command {
    private final ApplicationContext context;

    public HelpCommand(ApplicationContext context) {
        this.context = context;
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
        String text = context.getBeansOfType(Command.class).values().stream()
                .map(command -> command.command() + " - " + command.description())
                .collect(Collectors.joining("\n"));

        long chatId = update.message().chat().id();
        return new SendMessage(chatId, text);
    }
}
