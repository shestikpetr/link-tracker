package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.ChatState;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class UntrackCommand implements Command, StatefulCommand {

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Прекратить отслеживание ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        return new SendMessage(chatId, "");
    }

    @Override
    public Set<ChatState> handledStates() {
        return Set.of();
    }

    @Override
    public SendMessage handleInput(Update update) {
        return null;
    }
}
