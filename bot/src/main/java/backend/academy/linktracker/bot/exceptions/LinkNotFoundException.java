package backend.academy.linktracker.bot.exceptions;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException() {
        super("Ссылка не найдена.");
    }
}
