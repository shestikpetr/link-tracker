package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.StatefulCommand;
import backend.academy.linktracker.bot.state.ChatState;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
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
            long chatId = update.message().chat().id();
            try {
                botClient.execute(cmd.handleInput(update));
            } catch (Exception e) {
                log.error("Ошибка при обработке состояния {} для chatId={}", state, chatId, e);
                botClient.execute(new SendMessage(chatId, "Произошла внутренняя ошибка."));
            }
        }
    }
}
