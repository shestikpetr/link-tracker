package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1000)
public class CancelCommand implements Command {
    @Override
    public String command() {
        return "/cancel";
    }

    @Override
    public String description() {
        return "Отменить действие";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();

        return new SendMessage(chatId, "Действие отменено.");
    }
}
