package backend.academy.linktracker.scrapper.exceptions;

public class ChatAlreadyExistsException extends RuntimeException {
    public ChatAlreadyExistsException(Long chatId) {
        super("Чат уже зарегистрирован: " + chatId);
    }
}
