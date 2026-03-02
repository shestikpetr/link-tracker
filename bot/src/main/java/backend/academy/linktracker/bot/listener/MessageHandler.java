package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.command.Command;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MessageHandler {
    private final CommandRegistry commandRegistry;
    private final BotClient botClient;

    public MessageHandler(CommandRegistry commandRegistry, BotClient botClient) {
        this.commandRegistry = commandRegistry;
        this.botClient = botClient;
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
                chatId,
                "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."));
    }

    public void handle(Update update) {
        if (!isValid(update)) return;

        log.info(
                "Получено сообщение от chatId={}: {}",
                update.message().chat().id(),
                update.message().text());

        commandRegistry
                .find(parse(update.message().text()))
                .ifPresentOrElse(cmd -> executeCommand(cmd, update), () -> sendUnknownCommand(update));
    }
}
