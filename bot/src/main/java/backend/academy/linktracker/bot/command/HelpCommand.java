package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class HelpCommand implements Command {

    @Override
    public CommandInfo info() {
        return CommandInfo.HELP;
    }

    @Override
    public SendMessage handle(Update update) {
        String text = Arrays.stream(CommandInfo.values())
                .map(cmd -> cmd.command() + " - " + cmd.description())
                .collect(Collectors.joining("\n"));

        long chatId = update.message().chat().id();
        return new SendMessage(chatId, text);
    }
}
