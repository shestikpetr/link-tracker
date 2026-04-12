package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.LinkEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);

    @Modifying
    @Query("DELETE FROM LinkEntity l WHERE l.id NOT IN (SELECT cl.id.linkId FROM ChatLinkEntity cl)")
    void deleteOrphan();
}
