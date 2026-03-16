package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
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
    public void registerChat(Long chatId) {
        try {
            jdbcClient
                    .sql("INSERT INTO chats (id) VALUES (:chatId)")
                    .param("chatId", chatId)
                    .update();
        } catch (DuplicateKeyException e) {
            throw new ChatAlreadyExistsException(chatId);
        }
    }

    @Override
    public void deleteChat(Long chatId) {
        int rows = jdbcClient
                .sql("DELETE FROM chats WHERE id = :chatId")
                .param("chatId", chatId)
                .update();

        if (rows == 0) {
            throw new ChatNotFoundException(chatId);
        }
    }

    @Override
    public boolean existsChat(Long chatId) {
        return jdbcClient
                .sql("SELECT EXISTS(SELECT 1 FROM chats WHERE id = :chatId)")
                .param("chatId", chatId)
                .query(Boolean.class)
                .single();
    }
}
