package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
public class SqlLinkRepository implements LinkRepository {
    private final JdbcClient jdbcClient;

    @Override
    public TrackedLink addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        return null;
    }

    @Override
    public TrackedLink removeLink(Long chatId, URI url) {
        return null;
    }

    @Override
    public List<TrackedLink> findByChat(Long chatId) {
        return List.of();
    }

    @Override
    public Map<URI, List<ChatLink>> findAllGroupedByUrl() {
        return Map.of();
    }

    @Override
    public void deleteByChat(Long chatId) {}

    @Override
    public void updateLastChecked(Long linkId, Instant lastCheckedAt) {}
}
