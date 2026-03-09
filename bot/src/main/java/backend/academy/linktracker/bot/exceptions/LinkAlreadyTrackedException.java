package backend.academy.linktracker.bot.exceptions;

public class LinkAlreadyTrackedException extends RuntimeException {
    public LinkAlreadyTrackedException() {
        super("Ссылка уже отслеживается.");
    }
}
