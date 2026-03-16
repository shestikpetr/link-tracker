package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommandDispatcher {
    private static final String UNKNOWN_COMMAND_TEXT =
            "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.";

    private final CommandRegistry commandRegistry;
    private final BotClient botClient;

    public void dispatch(String commandName, Update update) {
        long chatId = update.message().chat().id();
        SendMessage response;
        try {
            response = commandRegistry
                    .find(commandName)
                    .map(cmd -> cmd.handle(update))
                    .orElseGet(() -> new SendMessage(chatId, UNKNOWN_COMMAND_TEXT));
        } catch (Exception e) {
            log.atError()
                    .setMessage("Ошибка при выполнении команды")
                    .addKeyValue("command", commandName)
                    .addKeyValue("chatId", chatId)
                    .setCause(e)
                    .log();
            response = new SendMessage(chatId, "Произошла внутренняя ошибка.");
        }
        botClient.execute(response);
    }

    public void sendUnknownCommand(Update update) {
        long chatId = update.message().chat().id();
        botClient.execute(new SendMessage(chatId, UNKNOWN_COMMAND_TEXT));
    }
}
