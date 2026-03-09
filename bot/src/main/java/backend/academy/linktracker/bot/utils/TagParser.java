package backend.academy.linktracker.bot.utils;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TagParser {
    public List<String> parseTags(String input) {
        return Arrays.stream(input.split(","))
                .map(this::normalize)
                .filter(t -> !t.isEmpty())
                .toList();
    }

    public String normalize(String tag) {
        return tag.trim().replaceAll("\\s+", " ");
    }
}
