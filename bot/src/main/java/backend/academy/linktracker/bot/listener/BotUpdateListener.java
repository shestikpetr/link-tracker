package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotUpdateListener implements UpdatesListener {
    private final TelegramBot telegramBot;
    private final CommandRegistry commandRegistry;

    public BotUpdateListener(TelegramBot telegramBot, CommandRegistry commandRegistry) {
        this.telegramBot = telegramBot;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public int process(List<Update> list) {
        list.forEach(update -> {
            if (update.message() == null
                    || update.message().text() == null
                    || update.message().text().isEmpty()) return;

            log.info(
                    "Получено сообщение от chatId={}: {}",
                    update.message().chat().id(),
                    update.message().text());

            commandRegistry
                    .find(update.message().text())
                    .ifPresentOrElse(
                            command -> {
                                log.debug("Выполнение команды: {}", command.command());
                                telegramBot.execute(command.handle(update));
                            },
                            () -> {
                                long chatId = update.message().chat().id();
                                telegramBot.execute(new SendMessage(
                                        chatId,
                                        "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных"
                                                + " команд."));
                            });
        });

        return CONFIRMED_UPDATES_ALL;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);

        telegramBot.execute(new SetMyCommands(commandRegistry.toBotCommands()));
    }
}
