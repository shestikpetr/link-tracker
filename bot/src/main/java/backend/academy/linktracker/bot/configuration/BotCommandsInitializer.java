package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.CommandRegistry;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotCommandsInitializer {
    private final BotClient botClient;
    private final CommandRegistry commandRegistry;

    @PostConstruct
    public void init() {
        botClient.execute(new SetMyCommands(commandRegistry.toBotCommands()));
    }
}
