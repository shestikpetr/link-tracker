package backend.academy.linktracker.scrapper.exception;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(Long chatId) {
        super("Чат не найден: " + chatId);
    }
}
