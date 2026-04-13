package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.LinkEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);

    @Query("SELECT l FROM LinkEntity l ORDER BY l.lastCheckedAt ASC LIMIT :limit")
    List<LinkEntity> findStaleLinks(@Param("limit") int limit);

    @Modifying
    @Query("DELETE FROM LinkEntity l WHERE l.id NOT IN (SELECT cl.id.linkId FROM ChatLinkEntity cl)")
    void deleteOrphan();
}
