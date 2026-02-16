package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.command.Command;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import java.util.List;

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
            if (update.message() == null
                    || update.message().text() == null
                    || update.message().text().isEmpty()) return;
            commands.stream()
                    .filter(command -> command.command().equals(update.message().text()))
                    .findFirst()
                    .ifPresent(command -> telegramBot.execute(command.handle(update)));
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }
}
