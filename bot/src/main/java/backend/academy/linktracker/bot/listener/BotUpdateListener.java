package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.command.Command;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
public class BotUpdateListener implements UpdatesListener {
    private final TelegramBot telegramBot;
    private final List<Command> commands;

    public BotUpdateListener(TelegramBot telegramBot, List<Command> commands) {
        this.telegramBot = telegramBot;
        this.commands = commands;
    }

    @Override
    public int process(List<Update> list) {
        list.forEach(update -> {
            log.info("{}: {}", update.message().chat().id(), update.message().text());

            if (update.message() == null
                    || update.message().text() == null
                    || update.message().text().isEmpty()) return;

            commands.stream()
                    .filter(command -> command.command().equals(update.message().text()))
                    .findFirst()
                    .ifPresentOrElse(
                            command -> {
                                log.debug("Выполнение команды: {}", command.command());
                                telegramBot.execute(command.handle(update));
                            },
                            () -> {
                                log.warn("Неизвестная команда: {}", update.message().text());
                                long chatId = update.message().chat().id();
                                telegramBot.execute(new SendMessage(
                                        chatId,
                                        "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных"
                                                + " команд."));
                            });
        });

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);

        BotCommand[] botCommands = commands.stream()
                .map(command -> new BotCommand(command.command(), command.description()))
                .toArray(BotCommand[]::new);

        telegramBot.execute(new SetMyCommands(botCommands));
    }
}
