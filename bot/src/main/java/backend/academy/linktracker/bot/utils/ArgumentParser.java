package backend.academy.linktracker.bot.utils;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ArgumentParser {
    public Optional<String> parseArguments(String text) {
        int spaceIndex = text.trim().indexOf(' ');
        if (spaceIndex == -1) {
            return Optional.empty();
        }
        String args = text.trim().substring(spaceIndex + 1).trim();
        return args.isEmpty() ? Optional.empty() : Optional.of(args);
    }
}
