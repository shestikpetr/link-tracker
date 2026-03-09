package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Update;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageHandler {
    private final CommandDispatcher commandDispatcher;
    private final StateInputDispatcher stateInputDispatcher;
    private final ChatStateService chatStateService;

    public void handle(Update update) {
        if (!isValid(update)) return;

        long chatId = update.message().chat().id();
        String text = update.message().text();

        log.info("Получено сообщение от chatId={}: {}", chatId, text);

        if (text.startsWith("/")) {
            chatStateService.clearState(chatId);
            commandDispatcher.dispatch(parse(text), update);
        } else {
            chatStateService
                    .getState(chatId)
                    .ifPresentOrElse(
                            state -> stateInputDispatcher.dispatch(state, update),
                            () -> commandDispatcher.sendUnknownCommand(update));
        }
    }

    private boolean isValid(Update update) {
        return update.message() != null
                && update.message().text() != null
                && !update.message().text().isEmpty();
    }

    private String parse(String text) {
        return text.trim().split("\\s+")[0].toLowerCase();
    }
}
