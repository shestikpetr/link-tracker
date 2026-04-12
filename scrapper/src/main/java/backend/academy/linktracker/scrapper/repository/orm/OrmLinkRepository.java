package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatLinkEntity;
import backend.academy.linktracker.scrapper.entity.ChatLinkId;
import backend.academy.linktracker.scrapper.entity.LinkEntity;
import backend.academy.linktracker.scrapper.entity.TagEntity;
import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "ORM")
public class OrmLinkRepository implements LinkRepository {
    private final JpaChatRepository jpaChatRepository;
    private final JpaLinkRepository jpaLinkRepository;
    private final JpaChatLinkRepository jpaChatLinkRepository;
    private final JpaTagRepository jpaTagRepository;

    @Override
    public Optional<TrackedLink> addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        LinkEntity link = jpaLinkRepository.findByUrl(url.toString()).orElseGet(() -> {
            LinkEntity l = new LinkEntity();
            l.setUrl(url.toString());
            return jpaLinkRepository.save(l);
        });

        ChatLinkId clId = new ChatLinkId(chatId, link.getId());
        if (jpaChatLinkRepository.existsById(clId)) {
            return Optional.empty();
        }

        ChatLinkEntity chatLink = new ChatLinkEntity();
        chatLink.setId(clId);
        chatLink.setChat(jpaChatRepository.getReferenceById(chatId));
        chatLink.setLink(link);
        chatLink.setFilters(filters.toArray(String[]::new));

        Set<TagEntity> tagEntities = new HashSet<>();
        for (String tagName : tags) {
            TagEntity tag = jpaTagRepository.findByName(tagName).orElseGet(() -> {
                TagEntity t = new TagEntity();
                t.setName(tagName);
                return jpaTagRepository.save(t);
            });
            tagEntities.add(tag);
        }
        chatLink.setTags(tagEntities);

        jpaChatLinkRepository.save(chatLink);

        return Optional.of(new TrackedLink(link.getId(), url, tags, filters, link.getLastCheckedAt()));
    }

    @Override
    public Optional<TrackedLink> removeLink(Long chatId, URI url) {
        Optional<LinkEntity> linkOpt = jpaLinkRepository.findByUrl(url.toString());
        if (linkOpt.isEmpty()) {
            return Optional.empty();
        }

        LinkEntity link = linkOpt.orElseThrow();
        ChatLinkId clId = new ChatLinkId(chatId, link.getId());
        Optional<ChatLinkEntity> chatLinkOpt = jpaChatLinkRepository.findById(clId);
        if (chatLinkOpt.isEmpty()) {
            return Optional.empty();
        }

        ChatLinkEntity chatLink = chatLinkOpt.orElseThrow();
        List<String> tags = chatLink.getTags().stream().map(TagEntity::getName).toList();
        List<String> linkFilters = List.of(chatLink.getFilters());

        jpaChatLinkRepository.delete(chatLink);
        jpaChatLinkRepository.flush();

        if (jpaChatLinkRepository.countByIdLinkId(link.getId()) == 0) {
            jpaLinkRepository.delete(link);
        }

        return Optional.of(new TrackedLink(link.getId(), url, tags, linkFilters, link.getLastCheckedAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TrackedLink> findByChat(Long chatId) {
        return jpaChatLinkRepository.findByIdChatIdWithLinkAndTags(chatId).stream()
                .map(cl -> new TrackedLink(
                        cl.getLink().getId(),
                        URI.create(cl.getLink().getUrl()),
                        cl.getTags().stream().map(TagEntity::getName).toList(),
                        List.of(cl.getFilters()),
                        cl.getLink().getLastCheckedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<URI, List<ChatLink>> findAllGroupedByUrl() {
        Map<URI, List<ChatLink>> result = new LinkedHashMap<>();

        for (ChatLinkEntity cl : jpaChatLinkRepository.findAllWithLinkAndTags()) {
            URI uri = URI.create(cl.getLink().getUrl());
            TrackedLink tracked = new TrackedLink(
                    cl.getLink().getId(),
                    uri,
                    cl.getTags().stream().map(TagEntity::getName).toList(),
                    List.of(cl.getFilters()),
                    cl.getLink().getLastCheckedAt());
            result.computeIfAbsent(uri, _ -> new ArrayList<>())
                    .add(new ChatLink(cl.getId().getChatId(), tracked));
        }

        return result;
    }

    @Override
    public void deleteByChat(Long chatId) {
        jpaChatLinkRepository.deleteByIdChatId(chatId);
        jpaChatLinkRepository.flush();
        jpaLinkRepository.deleteOrphan();
    }

    @Override
    public void updateLastChecked(Long linkId, Instant lastCheckedAt) {
        jpaLinkRepository.findById(linkId).ifPresent(link -> link.setLastCheckedAt(lastCheckedAt));
    }
}
