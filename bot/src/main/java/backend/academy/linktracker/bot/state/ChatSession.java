package backend.academy.linktracker.bot.state;

import java.net.URI;

public sealed interface ChatSession {
    record TrackUrl() implements ChatSession {}

    record TrackTags(URI url) implements ChatSession {}

    record Untrack() implements ChatSession {}
}
