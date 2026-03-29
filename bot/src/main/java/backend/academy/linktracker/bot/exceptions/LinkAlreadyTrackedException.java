package backend.academy.linktracker.bot.exceptions;

public class LinkAlreadyTrackedException extends RuntimeException {
    public LinkAlreadyTrackedException(String message) {
        super(message);
    }
}
