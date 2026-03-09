package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.StatefulCommand;
import backend.academy.linktracker.bot.state.ChatState;
import com.pengrad.telegrambot.model.Update;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StateInputDispatcher {
    private final Map<ChatState, StatefulCommand> stateHandlers;
    private final BotClient botClient;

    public StateInputDispatcher(List<StatefulCommand> statefulCommands, BotClient botClient) {
        this.botClient = botClient;
        this.stateHandlers = new EnumMap<>(ChatState.class);
        for (StatefulCommand cmd : statefulCommands) {
            for (ChatState state : cmd.handledStates()) {
                stateHandlers.put(state, cmd);
            }
        }
    }

    public void dispatch(ChatState state, Update update) {
        StatefulCommand cmd = stateHandlers.get(state);
        if (cmd != null) {
            botClient.execute(cmd.handleInput(update));
        }
    }
}
