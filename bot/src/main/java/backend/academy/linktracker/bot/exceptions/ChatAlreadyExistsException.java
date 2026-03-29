package backend.academy.linktracker.bot.exceptions;

public class ChatAlreadyExistsException extends RuntimeException {
    public ChatAlreadyExistsException(String message) {
        super(message);
    }
}
