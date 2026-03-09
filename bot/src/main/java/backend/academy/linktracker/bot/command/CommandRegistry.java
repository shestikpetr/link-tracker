package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.BotCommand;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommandRegistry {
    private final Map<String, Command> commands;

    public Optional<Command> find(String text) {
        return Optional.ofNullable(commands.get(text));
    }

    public BotCommand[] toBotCommands() {
        return commands.values().stream()
                .map(command -> new BotCommand(command.command(), command.description()))
                .toArray(BotCommand[]::new);
    }

    public Collection<Command> getAll() {
        return commands.values();
    }
}
