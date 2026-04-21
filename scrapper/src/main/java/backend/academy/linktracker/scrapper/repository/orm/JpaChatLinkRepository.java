package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatLinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaChatLinkRepository extends JpaRepository<ChatLinkEntity, Long> {
    boolean existsByChatIdAndLinkId(Long chatId, Long linkId);

    Optional<ChatLinkEntity> findByChatIdAndLinkId(Long chatId, Long linkId);

    void deleteByChatId(Long chatId);

    long countByLinkId(Long linkId);

    @Query("""
        SELECT cl FROM ChatLinkEntity cl
        JOIN FETCH cl.link
        LEFT JOIN FETCH cl.tags
        WHERE cl.chat.id = :chatId
        """)
    List<ChatLinkEntity> findByChatIdWithLinkAndTags(@Param("chatId") Long chatId);

    @Query("""
        SELECT DISTINCT cl FROM ChatLinkEntity cl
        JOIN FETCH cl.link
        LEFT JOIN FETCH cl.tags
        WHERE cl.chat.id = :chatId
        AND EXISTS (
        SELECT 1 FROM cl.tags t WHERE t.name IN :tags
        )
        """)
    List<ChatLinkEntity> findByChatIdAndTagNames(@Param("chatId") Long chatId, @Param("tags") List<String> tags);

    @Query("""
        SELECT cl FROM ChatLinkEntity cl
        JOIN FETCH cl.link
        LEFT JOIN FETCH cl.tags
        """)
    List<ChatLinkEntity> findAllWithLinkAndTags();
}
