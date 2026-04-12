package backend.academy.linktracker.scrapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
abstract class AbstractRepositoryTest {

    static final URI GITHUB_URL = URI.create("https://github.com/test/repo");
    static final URI GITHUB_URL_2 = URI.create("https://github.com/test/repo2");

    @Autowired
    ChatRepository chatRepository;

    @Autowired
    LinkRepository linkRepository;

    @Test
    void registerChat_success() {
        chatRepository.registerChat(1L);

        assertThat(chatRepository.chatExists(1L)).isTrue();
    }

    @Test
    void registerChat_duplicate_returns_false() {
        chatRepository.registerChat(1L);

        assertThat(chatRepository.registerChat(1L)).isFalse();
    }

    @Test
    void deleteChat_success() {
        chatRepository.registerChat(1L);
        chatRepository.deleteChat(1L);

        assertThat(chatRepository.chatExists(1L)).isFalse();
    }

    @Test
    void deleteChat_nonexistent_returns_false() {
        assertThat(chatRepository.deleteChat(999L)).isFalse();
    }

    @Test
    void chatExists_returns_false_for_unknown() {
        assertThat(chatRepository.chatExists(999L)).isFalse();
    }

    @Test
    void addLink_success() {
        chatRepository.registerChat(1L);

        TrackedLink link = linkRepository
                .addLink(1L, GITHUB_URL, List.of("tag1"), List.of())
                .orElseThrow();

        assertThat(link.url()).isEqualTo(GITHUB_URL);
        assertThat(link.tags()).containsExactly("tag1");
        assertThat(link.id()).isNotNull();
    }

    @Test
    void addLink_duplicate_returns_empty() {
        chatRepository.registerChat(1L);
        linkRepository.addLink(1L, GITHUB_URL, List.of(), List.of());

        assertThat(linkRepository.addLink(1L, GITHUB_URL, List.of(), List.of())).isEmpty();
    }

    @Test
    void removeLink_success() {
        chatRepository.registerChat(1L);
        linkRepository.addLink(1L, GITHUB_URL, List.of("tag1"), List.of());

        TrackedLink removed = linkRepository.removeLink(1L, GITHUB_URL).orElseThrow();

        assertThat(removed.url()).isEqualTo(GITHUB_URL);
        assertThat(linkRepository.findByChat(1L)).isEmpty();
    }

    @Test
    void removeLink_nonexistent_returns_empty() {
        chatRepository.registerChat(1L);

        assertThat(linkRepository.removeLink(1L, GITHUB_URL)).isEmpty();
    }

    @Test
    void findByChat_returns_links() {
        chatRepository.registerChat(1L);
        linkRepository.addLink(1L, GITHUB_URL, List.of("tag1"), List.of());
        linkRepository.addLink(1L, GITHUB_URL_2, List.of("tag2"), List.of());

        Collection<TrackedLink> links = linkRepository.findByChat(1L);

        assertThat(links).hasSize(2);
        assertThat(links).extracting(TrackedLink::url).containsExactlyInAnyOrder(GITHUB_URL, GITHUB_URL_2);
    }

    @Test
    void findByChat_nonexistent_chat_returns_empty() {
        assertThat(linkRepository.findByChat(999L)).isEmpty();
    }

    @Test
    void findByChat_empty_when_no_links() {
        chatRepository.registerChat(1L);

        assertThat(linkRepository.findByChat(1L)).isEmpty();
    }

    @Test
    void findAllGroupedByUrl_groups_correctly() {
        chatRepository.registerChat(1L);
        chatRepository.registerChat(2L);
        linkRepository.addLink(1L, GITHUB_URL, List.of(), List.of());
        linkRepository.addLink(2L, GITHUB_URL, List.of(), List.of());

        Map<URI, List<ChatLink>> grouped = linkRepository.findAllGroupedByUrl();

        assertThat(grouped).containsKey(GITHUB_URL);
        assertThat(grouped.get(GITHUB_URL)).hasSize(2);
        assertThat(grouped.get(GITHUB_URL)).extracting(ChatLink::chatId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void deleteByChat_removes_links() {
        chatRepository.registerChat(1L);
        linkRepository.addLink(1L, GITHUB_URL, List.of(), List.of());

        linkRepository.deleteByChat(1L);

        assertThat(linkRepository.findByChat(1L)).isEmpty();
    }

    @Test
    void updateLastChecked_updates_timestamp() {
        chatRepository.registerChat(1L);
        TrackedLink link =
                linkRepository.addLink(1L, GITHUB_URL, List.of(), List.of()).orElseThrow();

        Instant newTime = Instant.parse("2025-01-01T00:00:00Z");
        linkRepository.updateLastChecked(link.id(), newTime);

        Collection<TrackedLink> links = linkRepository.findByChat(1L);
        assertThat(new ArrayList<>(links).getFirst().lastCheckedAt()).isEqualTo(newTime);
    }

    @Test
    void addLink_with_tags_persists_tags() {
        chatRepository.registerChat(1L);

        linkRepository.addLink(1L, GITHUB_URL, List.of("java", "spring"), List.of());

        Collection<TrackedLink> links = linkRepository.findByChat(1L);
        assertThat(new ArrayList<>(links).getFirst().tags()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void same_url_different_chats_different_tags() {
        chatRepository.registerChat(1L);
        chatRepository.registerChat(2L);
        linkRepository.addLink(1L, GITHUB_URL, List.of("tag1"), List.of());
        linkRepository.addLink(2L, GITHUB_URL, List.of("tag2"), List.of());

        Collection<TrackedLink> links1 = linkRepository.findByChat(1L);
        Collection<TrackedLink> links2 = linkRepository.findByChat(2L);

        assertThat(new ArrayList<>(links1).getFirst().tags()).containsExactly("tag1");
        assertThat(new ArrayList<>(links2).getFirst().tags()).containsExactly("tag2");
    }
}
