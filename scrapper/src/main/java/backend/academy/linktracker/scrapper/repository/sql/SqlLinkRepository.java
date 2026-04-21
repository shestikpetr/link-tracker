package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "SQL")
public class SqlLinkRepository implements LinkRepository {
    private final JdbcClient jdbcClient;

    private record LinkRow(Long chatId, Long id, String url, Instant lastCheckedAt, String[] filters, String tagName) {}

    @Override
    public Optional<TrackedLink> addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        var linkRow = jdbcClient
                .sql("""
                    INSERT INTO links (url) VALUES (:url)
                    ON CONFLICT (url) DO UPDATE SET url = EXCLUDED.url
                    RETURNING id, last_checked_at
                    """)
                .param("url", url.toString())
                .query((rs, _) -> Map.entry(
                        rs.getLong("id"), rs.getTimestamp("last_checked_at").toInstant()))
                .single();

        Long linkId = linkRow.getKey();
        Instant lastCheckedAt = linkRow.getValue();

        Long chatLinkId;
        try {
            chatLinkId = jdbcClient
                    .sql("""
                        INSERT INTO chat_links (chat_id, link_id, filters) VALUES (:chatId, :linkId, :filters)
                        RETURNING id
                        """)
                    .param("chatId", chatId)
                    .param("linkId", linkId)
                    .param("filters", filters.toArray(String[]::new))
                    .query(Long.class)
                    .single();
        } catch (DuplicateKeyException e) {
            return Optional.empty();
        }

        for (String tag : new LinkedHashSet<>(tags)) {
            Long tagId =
                    jdbcClient.sql("""
                        INSERT INTO tags (name) VALUES (:name)
                        ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                        RETURNING id
                        """).param("name", tag).query(Long.class).single();

            jdbcClient
                    .sql("INSERT INTO link_tags (chat_link_id, tag_id) VALUES (:chatLinkId, :tagId)")
                    .param("chatLinkId", chatLinkId)
                    .param("tagId", tagId)
                    .update();
        }

        return Optional.of(new TrackedLink(linkId, url, tags, filters, lastCheckedAt));
    }

    @Override
    public Optional<TrackedLink> updateLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        var linkRow = jdbcClient
                .sql("SELECT id, last_checked_at FROM links WHERE url = :url")
                .param("url", url.toString())
                .query((rs, _) -> Map.entry(
                        rs.getLong("id"), rs.getTimestamp("last_checked_at").toInstant()))
                .optional();

        if (linkRow.isEmpty()) {
            return Optional.empty();
        }

        Long linkId = linkRow.orElseThrow().getKey();
        Instant lastCheckedAt = linkRow.orElseThrow().getValue();

        Optional<Long> chatLinkIdOpt = jdbcClient
                .sql("SELECT id FROM chat_links WHERE chat_id = :chatId AND link_id = :linkId")
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query(Long.class)
                .optional();

        if (chatLinkIdOpt.isEmpty()) {
            return Optional.empty();
        }

        Long chatLinkId = chatLinkIdOpt.orElseThrow();

        jdbcClient
                .sql("UPDATE chat_links SET filters = :filters WHERE id = :chatLinkId")
                .param("filters", filters.toArray(String[]::new))
                .param("chatLinkId", chatLinkId)
                .update();

        jdbcClient
                .sql("DELETE FROM link_tags WHERE chat_link_id = :chatLinkId")
                .param("chatLinkId", chatLinkId)
                .update();

        for (String tag : new LinkedHashSet<>(tags)) {
            Long tagId =
                    jdbcClient.sql("""
                        INSERT INTO tags (name) VALUES (:name)
                        ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name
                        RETURNING id
                        """).param("name", tag).query(Long.class).single();

            jdbcClient
                    .sql("INSERT INTO link_tags (chat_link_id, tag_id) VALUES (:chatLinkId, :tagId)")
                    .param("chatLinkId", chatLinkId)
                    .param("tagId", tagId)
                    .update();
        }

        return Optional.of(new TrackedLink(linkId, url, tags, filters, lastCheckedAt));
    }

    @Override
    public Optional<TrackedLink> removeLink(Long chatId, URI url) {
        var linkRow = jdbcClient
                .sql("SELECT id, last_checked_at FROM links WHERE url = :url")
                .param("url", url.toString())
                .query((rs, _) -> Map.entry(
                        rs.getLong("id"), rs.getTimestamp("last_checked_at").toInstant()))
                .optional();

        if (linkRow.isEmpty()) {
            return Optional.empty();
        }

        Long linkId = linkRow.orElseThrow().getKey();
        Instant lastCheckedAt = linkRow.orElseThrow().getValue();

        var chatLinkRow = jdbcClient
                .sql("""
                    SELECT id, filters FROM chat_links
                    WHERE chat_id = :chatId AND link_id = :linkId
                    """)
                .param("chatId", chatId)
                .param("linkId", linkId)
                .query((rs, _) -> Map.entry(
                        rs.getLong("id"), (String[]) rs.getArray("filters").getArray()))
                .optional();

        if (chatLinkRow.isEmpty()) {
            return Optional.empty();
        }

        Long chatLinkId = chatLinkRow.orElseThrow().getKey();
        List<String> linkFilters = List.of(chatLinkRow.orElseThrow().getValue());

        List<String> tags = jdbcClient
                .sql("""
                    SELECT t.name FROM tags t
                    JOIN link_tags lt ON t.id = lt.tag_id
                    WHERE lt.chat_link_id = :chatLinkId
                    """)
                .param("chatLinkId", chatLinkId)
                .query(String.class)
                .list();

        jdbcClient
                .sql("DELETE FROM chat_links WHERE id = :chatLinkId")
                .param("chatLinkId", chatLinkId)
                .update();

        jdbcClient.sql("""
            DELETE FROM links
            WHERE id = :linkId
            AND NOT EXISTS (SELECT 1 FROM chat_links WHERE link_id = :linkId)
            """).param("linkId", linkId).update();

        return Optional.of(new TrackedLink(linkId, url, tags, linkFilters, lastCheckedAt));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TrackedLink> findByChat(Long chatId) {
        var rows = jdbcClient
                .sql("""
                    SELECT l.id, l.url, l.last_checked_at, cl.filters, t.name AS tag_name
                    FROM links l
                    JOIN chat_links cl ON l.id = cl.link_id
                    LEFT JOIN link_tags lt ON lt.chat_link_id = cl.id
                    LEFT JOIN tags t ON t.id = lt.tag_id
                    WHERE cl.chat_id = :chatId
                    """)
                .param("chatId", chatId)
                .query((rs, _) -> mapLinkRow(chatId, rs))
                .list();

        return groupToTrackedLinks(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TrackedLink> findByChatAndTags(Long chatId, List<String> tags) {
        var rows = jdbcClient
                .sql("""
                    SELECT DISTINCT l.id, l.url, l.last_checked_at, cl.filters, t2.name AS tag_name
                    FROM links l
                    JOIN chat_links cl ON l.id = cl.link_id
                    JOIN link_tags lt2 ON lt2.chat_link_id = cl.id
                    JOIN tags t_filter ON t_filter.id = lt2.tag_id AND t_filter.name IN (:tags)
                    LEFT JOIN link_tags lt ON lt.chat_link_id = cl.id
                    LEFT JOIN tags t2 ON t2.id = lt.tag_id
                    WHERE cl.chat_id = :chatId
                    """)
                .param("chatId", chatId)
                .param("tags", tags)
                .query((rs, _) -> mapLinkRow(chatId, rs))
                .list();

        return groupToTrackedLinks(rows);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<URI, List<ChatLink>> findAllGroupedByUrl() {
        var rows = jdbcClient
                .sql("""
                    SELECT cl.chat_id, l.id, l.url, l.last_checked_at, cl.filters, t.name AS tag_name
                    FROM links l
                    JOIN chat_links cl ON l.id = cl.link_id
                    LEFT JOIN link_tags lt ON lt.chat_link_id = cl.id
                    LEFT JOIN tags t ON t.id = lt.tag_id
                    """)
                .query((rs, _) -> mapLinkRow(rs.getLong("chat_id"), rs))
                .list();

        Map<Long, Map<Long, List<LinkRow>>> byChatAndLink =
                rows.stream().collect(Collectors.groupingBy(LinkRow::chatId, Collectors.groupingBy(LinkRow::id)));
        Map<URI, List<ChatLink>> result = new LinkedHashMap<>();

        for (var chatEntry : byChatAndLink.entrySet()) {
            Long chatId = chatEntry.getKey();

            for (var linkEntry : chatEntry.getValue().entrySet()) {
                TrackedLink trackedLink = toTrackedLink(linkEntry.getValue());

                result.computeIfAbsent(trackedLink.url(), _ -> new ArrayList<>())
                        .add(new ChatLink(chatId, trackedLink));
            }
        }

        return result;
    }

    @Override
    public void deleteByChat(Long chatId) {
        jdbcClient
                .sql("DELETE FROM chat_links WHERE chat_id = :chatId")
                .param("chatId", chatId)
                .update();

        jdbcClient
                .sql("DELETE FROM links WHERE NOT EXISTS (SELECT 1 FROM chat_links WHERE link_id = links.id)")
                .update();
    }

    private LinkRow mapLinkRow(Long chatId, ResultSet rs) throws SQLException {
        return new LinkRow(
                chatId,
                rs.getLong("id"),
                rs.getString("url"),
                rs.getTimestamp("last_checked_at").toInstant(),
                rs.getArray("filters") != null
                        ? (String[]) rs.getArray("filters").getArray()
                        : new String[0],
                rs.getString("tag_name"));
    }

    private Collection<TrackedLink> groupToTrackedLinks(List<LinkRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(LinkRow::id, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(SqlLinkRepository::toTrackedLink)
                .toList();
    }

    private static TrackedLink toTrackedLink(List<LinkRow> group) {
        var first = group.getFirst();
        List<String> tags =
                group.stream().map(LinkRow::tagName).filter(Objects::nonNull).toList();

        return new TrackedLink(
                first.id(), URI.create(first.url()), tags, List.of(first.filters()), first.lastCheckedAt());
    }

    @Override
    public void updateLastChecked(Long linkId, Instant lastCheckedAt) {
        jdbcClient
                .sql("UPDATE links SET last_checked_at = :lastCheckedAt WHERE id = :linkId")
                .param("lastCheckedAt", OffsetDateTime.ofInstant(lastCheckedAt, ZoneOffset.UTC))
                .param("linkId", linkId)
                .update();
    }
}
