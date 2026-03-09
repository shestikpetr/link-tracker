package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandDispatcher {
    private final CommandRegistry commandRegistry;
    private final BotClient botClient;

    public void dispatch(String commandName, Update update) {
        commandRegistry
                .find(commandName)
                .ifPresentOrElse(cmd -> botClient.execute(cmd.handle(update)), () -> sendUnknownCommand(update));
    }

    public void sendUnknownCommand(Update update) {
        long chatId = update.message().chat().id();
        botClient.execute(new SendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."));
    }
}
