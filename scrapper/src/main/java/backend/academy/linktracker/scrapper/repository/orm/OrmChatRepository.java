package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
public class OrmChatRepository implements ChatRepository {
    @Override
    public void registerChat(Long chatId) {}

    @Override
    public void deleteChat(Long chatId) {}

    @Override
    public boolean existsChat(Long chatId) {
        return false;
    }
}
