package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class BotCommandsInitializer {
    private final BotClient botClient;
    private final CommandRegistry commandRegistry;

    public BotCommandsInitializer(BotClient botClient, CommandRegistry commandRegistry) {
        this.botClient = botClient;
        this.commandRegistry = commandRegistry;
    }

    @PostConstruct
    public void init() {
        botClient.execute(new SetMyCommands(commandRegistry.toBotCommands()));
    }
}
