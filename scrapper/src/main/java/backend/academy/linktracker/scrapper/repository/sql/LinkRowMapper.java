package backend.academy.linktracker.scrapper.repository.sql;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

@Component
class LinkRowMapper implements RowMapper<LinkRow> {

    @Override
    public LinkRow mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new LinkRow(
                rs.getLong("chat_id"),
                rs.getLong("id"),
                rs.getString("url"),
                rs.getTimestamp("last_checked_at").toInstant(),
                rs.getArray("filters") != null
                        ? (String[]) rs.getArray("filters").getArray()
                        : new String[0],
                rs.getString("tag_name"));
    }

    List<TrackedLink> groupRowsByLink(List<LinkRow> rows) {
        return rows.stream()
                .collect(Collectors.groupingBy(LinkRow::id, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(group -> {
                    var first = group.getFirst();
                    List<String> tags = group.stream()
                            .map(LinkRow::tagName)
                            .filter(Objects::nonNull)
                            .toList();
                    return new TrackedLink(
                            first.id(), URI.create(first.url()), tags, List.of(first.filters()), first.lastCheckedAt());
                })
                .toList();
    }

    Map<URI, List<ChatLink>> groupRowsByChatLink(List<LinkRow> rows) {
        Map<Long, Map<Long, List<LinkRow>>> byChatAndLink =
                rows.stream().collect(Collectors.groupingBy(LinkRow::chatId, Collectors.groupingBy(LinkRow::id)));

        Map<URI, List<ChatLink>> result = new LinkedHashMap<>();
        for (var chatEntry : byChatAndLink.entrySet()) {
            Long chatId = chatEntry.getKey();
            for (var linkEntry : chatEntry.getValue().entrySet()) {
                List<LinkRow> group = linkEntry.getValue();
                var first = group.getFirst();
                List<String> tags = group.stream()
                        .map(LinkRow::tagName)
                        .filter(Objects::nonNull)
                        .toList();
                TrackedLink trackedLink = new TrackedLink(
                        first.id(), URI.create(first.url()), tags, List.of(first.filters()), first.lastCheckedAt());
                result.computeIfAbsent(trackedLink.url(), _ -> new ArrayList<>())
                        .add(new ChatLink(chatId, trackedLink));
            }
        }
        return result;
    }
}
