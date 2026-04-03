package backend.academy.linktracker.bot.exceptions;

public class UnsupportedLinkException extends RuntimeException {
    public UnsupportedLinkException(String message) {
        super(message);
    }
}
