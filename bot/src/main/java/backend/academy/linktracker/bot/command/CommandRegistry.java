package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.BotCommand;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CommandRegistry {
    private final Map<String, Command> commands;

    public CommandRegistry(List<Command> commands) {
        this.commands = commands.stream()
                .sorted(Comparator.comparingInt(Command::order))
                .collect(Collectors.toMap(Command::command, c -> c, (a, _) -> a, LinkedHashMap::new));
    }

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
