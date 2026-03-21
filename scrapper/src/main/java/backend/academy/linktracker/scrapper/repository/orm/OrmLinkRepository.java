package backend.academy.linktracker.scrapper.repository.orm;

import backend.academy.linktracker.scrapper.entity.ChatLinkEntity;
import backend.academy.linktracker.scrapper.entity.ChatLinkId;
import backend.academy.linktracker.scrapper.entity.LinkEntity;
import backend.academy.linktracker.scrapper.entity.TagEntity;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final ChatRepository chatRepository;

    @Override
    public TrackedLink addLink(Long chatId, URI url, List<String> tags, List<String> filters) {
        if (!chatRepository.chatExists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        LinkEntity link = jpaLinkRepository.findByUrl(url.toString()).orElseGet(() -> {
            LinkEntity l = new LinkEntity();
            l.setUrl(url.toString());
            return jpaLinkRepository.save(l);
        });

        ChatLinkId clId = new ChatLinkId(chatId, link.getId());
        if (jpaChatLinkRepository.existsById(clId)) {
            throw new LinkAlreadyExistsException(url);
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

        return new TrackedLink(link.getId(), url, tags, filters, link.getLastCheckedAt());
    }

    @Override
    public TrackedLink removeLink(Long chatId, URI url) {
        if (!chatRepository.chatExists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

        LinkEntity link = jpaLinkRepository.findByUrl(url.toString()).orElseThrow(() -> new LinkNotFoundException(url));

        ChatLinkId clId = new ChatLinkId(chatId, link.getId());
        ChatLinkEntity chatLink =
                jpaChatLinkRepository.findById(clId).orElseThrow(() -> new LinkNotFoundException(url));

        List<String> tags = chatLink.getTags().stream().map(TagEntity::getName).toList();
        List<String> filters = List.of(chatLink.getFilters());

        jpaChatLinkRepository.delete(chatLink);
        jpaChatLinkRepository.flush();

        if (jpaChatLinkRepository.countByIdLinkId(link.getId()) == 0) {
            jpaLinkRepository.delete(link);
        }

        return new TrackedLink(link.getId(), url, tags, filters, link.getLastCheckedAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrackedLink> findByChat(Long chatId) {
        if (!chatRepository.chatExists(chatId)) {
            throw new ChatNotFoundException(chatId);
        }

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
    }

    @Override
    public void updateLastChecked(Long linkId, Instant lastCheckedAt) {
        jpaLinkRepository.findById(linkId).ifPresent(link -> link.setLastCheckedAt(lastCheckedAt));
    }
}
