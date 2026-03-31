package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.repository.ChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
public class SqlChatRepository implements ChatRepository {
    private final JdbcClient jdbcClient;

    @Override
    public boolean registerChat(Long chatId) {
        try {
            jdbcClient
                    .sql("INSERT INTO chats (id) VALUES (:chatId)")
                    .param("chatId", chatId)
                    .update();
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public boolean deleteChat(Long chatId) {
        int rows = jdbcClient
                .sql("DELETE FROM chats WHERE id = :chatId")
                .param("chatId", chatId)
                .update();
        return rows > 0;
    }

    @Override
    public boolean chatExists(Long chatId) {
        return jdbcClient
                .sql("SELECT EXISTS(SELECT 1 FROM chats WHERE id = :chatId)")
                .param("chatId", chatId)
                .query(Boolean.class)
                .single();
    }
}
