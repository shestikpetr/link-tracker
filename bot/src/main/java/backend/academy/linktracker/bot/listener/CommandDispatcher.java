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
    private final CommandRegistry commandRegistry;
    private final BotClient botClient;

    public void dispatch(String commandName, Update update) {
        long chatId = update.message().chat().id();
        try {
            commandRegistry
                    .find(commandName)
                    .ifPresentOrElse(cmd -> botClient.execute(cmd.handle(update)), () -> sendUnknownCommand(update));
        } catch (Exception e) {
            log.error("Ошибка при выполнении команды '{}' для chatId={}", commandName, chatId, e);
            botClient.execute(new SendMessage(chatId, "Произошла внутренняя ошибка."));
        }
    }

    public void sendUnknownCommand(Update update) {
        long chatId = update.message().chat().id();
        botClient.execute(new SendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."));
    }
}
