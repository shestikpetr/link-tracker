package backend.academy.linktracker.scrapper.repository;

public interface ChatRepository {
    void registerChat(Long chatId);

    void deleteChat(Long chatId);

    boolean chatExists(Long chatId);
}
