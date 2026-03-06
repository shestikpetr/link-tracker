package backend.academy.linktracker.scrapper.exceptions;

public class ChatNotFoundException extends RuntimeException {
    public ChatNotFoundException(Long chatId) {
        super("Чат не найден: " + chatId);
    }
}
