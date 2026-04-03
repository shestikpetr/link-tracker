package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.StateHandler;
import backend.academy.linktracker.bot.state.ChatSession;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StateInputDispatcher {
    private final Map<Class<? extends ChatSession>, StateHandler> sessionHandlers;
    private final BotClient botClient;

    public StateInputDispatcher(List<StateHandler> stateHandlers, BotClient botClient) {
        this.botClient = botClient;
        this.sessionHandlers = stateHandlers.stream()
                .collect(Collectors.toMap(
                        StateHandler::handledSessionType, handler -> handler, (_, b) -> b, HashMap::new));
    }

    public void dispatch(ChatSession session, Update update) {
        StateHandler cmd = sessionHandlers.get(session.getClass());

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
                    .addKeyValue("session", session)
                    .addKeyValue("chatId", chatId)
                    .setCause(e)
                    .log();
            response = new SendMessage(chatId, "Произошла внутренняя ошибка.");
        }

        botClient.execute(response);
    }
}
