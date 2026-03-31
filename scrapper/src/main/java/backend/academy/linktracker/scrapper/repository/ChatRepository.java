package backend.academy.linktracker.scrapper.repository;

public interface ChatRepository {
    boolean registerChat(Long chatId);

    boolean deleteChat(Long chatId);

    boolean chatExists(Long chatId);
}
