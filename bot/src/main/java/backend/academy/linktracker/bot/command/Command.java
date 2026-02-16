package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

interface Command {
    String command();
    String description();
    SendMessage handle(Update update);
}
