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
        SendMessage response;
        try {
            response = commandRegistry.find(commandName)
                    .map(cmd -> cmd.handle(update))
                    .orElseGet(() -> new SendMessage(chatId,
                            "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."));
        } catch (Exception e) {
            log.error("Ошибка при выполнении команды '{}' для chatId={}", commandName, chatId, e);
            response = new SendMessage(chatId, "Произошла внутренняя ошибка.");
        }
        botClient.execute(response);
    }

    public void sendUnknownCommand(Update update) {
        long chatId = update.message().chat().id();
        botClient.execute(new SendMessage(
                chatId, "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."));
    }
}
