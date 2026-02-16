package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

class StartCommand implements Command {

    @Override
    public String command() {
        return "";
    }

    @Override
    public String description() {
        return "";
    }

    @Override
    public SendMessage handle(Update update) {
        return null;
    }
}
