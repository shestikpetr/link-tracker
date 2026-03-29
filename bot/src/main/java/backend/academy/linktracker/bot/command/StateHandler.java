package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.ChatState;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;

public interface StateHandler {
    ChatState handledState();

    SendMessage handleInput(Update update);
}
