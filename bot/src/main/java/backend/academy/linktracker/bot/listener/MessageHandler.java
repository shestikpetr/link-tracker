package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.command.StatefulCommand;
import backend.academy.linktracker.bot.state.ChatState;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageHandler {
    private final CommandRegistry commandRegistry;
    private final BotClient botClient;
    private final ChatStateService chatStateService;
    private final Map<ChatState, StatefulCommand> stateHandlers;

    public MessageHandler(
            CommandRegistry commandRegistry,
            BotClient botClient,
            ChatStateService chatStateService,
            List<StatefulCommand> statefulCommands) {
        this.commandRegistry = commandRegistry;
        this.botClient = botClient;
        this.chatStateService = chatStateService;
        this.stateHandlers = new EnumMap<>(ChatState.class);
        for (StatefulCommand cmd : statefulCommands) {
            for (ChatState state : cmd.handledStates()) {
                stateHandlers.put(state, cmd);
            }
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

    private void executeCommand(Command cmd, Update update) {
        botClient.execute(cmd.handle(update));
    }

    private void sendUnknownCommand(Update update) {
        long chatId = update.message().chat().id();
        botClient.execute(new SendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."));
    }

    private boolean isCommand(Update update) {
        return update.message().text().startsWith("/");
    }

    public void handle(Update update) {
        if (!isValid(update)) return;

        log.info(
                "Получено сообщение от chatId={}: {}",
                update.message().chat().id(),
                update.message().text());

        if (isCommand(update)) {
            chatStateService.clearState(update.message().chat().id());
            commandRegistry
                    .find(parse(update.message().text()))
                    .ifPresentOrElse(cmd -> executeCommand(cmd, update), () -> sendUnknownCommand(update));
        } else {
            chatStateService
                    .getState(update.message().chat().id())
                    .ifPresentOrElse(state -> handleStateInput(state, update), () -> sendUnknownCommand(update));
        }
    }

    private void handleStateInput(ChatState state, Update update) {
        StatefulCommand cmd = stateHandlers.get(state);
        if (cmd != null) botClient.execute(cmd.handleInput(update));
    }
}
