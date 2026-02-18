package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

public interface Command {
    CommandInfo info();

    default String command() {
        return info().command();
    }

    default String description() {
        return info().description();
    }

    SendMessage handle(Update update);
}
