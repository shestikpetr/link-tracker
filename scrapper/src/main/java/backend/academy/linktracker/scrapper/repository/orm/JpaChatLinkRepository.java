package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatLinkEntity;
import backend.academy.linktracker.scrapper.entity.ChatLinkId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaChatLinkRepository extends JpaRepository<ChatLinkEntity, ChatLinkId> {
    List<ChatLinkEntity> findByIdChatId(Long chatId);

    void deleteByIdChatId(Long chatId);

    boolean existsByIdChatIdAndIdLinkId(Long chatId, Long linkId);

    @Query("""
        SELECT cl FROM ChatLinkEntity cl
        JOIN FETCH cl.link
        LEFT JOIN FETCH cl.tags
        WHERE cl.id.chatId = :chatId
        """)
    List<ChatLinkEntity> findByIdChatIdWithLinkAndTags(@Param("chatId") Long chatId);

    @Query("""
        SELECT DISTINCT cl FROM ChatLinkEntity cl
        JOIN FETCH cl.link
        LEFT JOIN FETCH cl.tags
        WHERE cl.id.chatId = :chatId
        AND EXISTS (
        SELECT 1 FROM cl.tags t WHERE t.name IN :tags
        )
        """)
    List<ChatLinkEntity> findByIdChatIdAndTagNames(@Param("chatId") Long chatId, @Param("tags") List<String> tags);

    @Query("""
        SELECT cl FROM ChatLinkEntity cl
        JOIN FETCH cl.link
        LEFT JOIN FETCH cl.tags
        """)
    List<ChatLinkEntity> findAllWithLinkAndTags();

    long countByIdLinkId(Long linkId);
}
