package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.StateHandler;
import backend.academy.linktracker.bot.state.ChatState;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StateInputDispatcher {
    private final Map<ChatState, StateHandler> stateHandlers;
    private final BotClient botClient;

    public StateInputDispatcher(List<StateHandler> stateHandlers, BotClient botClient) {
        this.botClient = botClient;
        this.stateHandlers = stateHandlers.stream()
                .collect(Collectors.toMap(
                        StateHandler::handledState,
                        handler -> handler,
                        (_, b) -> b,
                        () -> new EnumMap<>(ChatState.class)));
    }

    public void dispatch(ChatState state, Update update) {
        StateHandler cmd = stateHandlers.get(state);

        if (cmd == null) {
            return;
        }

        long chatId = update.message().chat().id();
        SendMessage response;

        try {
            response = cmd.handleInput(update);
        } catch (Exception e) {
            log.atError()
                    .setMessage("Ошибка при обработке состояния")
                    .addKeyValue("state", state)
                    .addKeyValue("chatId", chatId)
                    .setCause(e)
                    .log();
            response = new SendMessage(chatId, "Произошла внутренняя ошибка.");
        }

        botClient.execute(response);
    }
}
