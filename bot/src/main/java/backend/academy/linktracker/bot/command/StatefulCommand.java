package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.ChatState;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.Set;

public interface StatefulCommand {
    Set<ChatState> handledStates();

    SendMessage handleInput(Update update);
}
