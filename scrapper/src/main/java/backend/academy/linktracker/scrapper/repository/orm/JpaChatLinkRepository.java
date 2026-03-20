package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatLinkEntity;
import backend.academy.linktracker.scrapper.entity.ChatLinkId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaChatLinkRepository extends JpaRepository<ChatLinkEntity, ChatLinkId> {
    List<ChatLinkEntity> findByIdChatId(Long chatId);

    void deleteByIdChatId(Long chatId);

    boolean existsByIdChatIdAndIdLinkId(Long chatId, Long linkId);
}
