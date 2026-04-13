package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.ChatSession;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

public interface StateHandler {
    Class<? extends ChatSession> handledSessionType();

    SendMessage handleInput(Update update);
}
