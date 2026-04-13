package backend.academy.linktracker.bot.exceptions;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(String message) {
        super(message);
    }
}
