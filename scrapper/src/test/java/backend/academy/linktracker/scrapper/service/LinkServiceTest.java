package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.dto.LinkResponse;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    static final URI GITHUB_URL = URI.create("https://github.com/foo/bar");
    static final URI UNSUPPORTED_URL = URI.create("https://unsupported.com/foo");

    @Mock
    LinkRepository linkRepository;

    @Mock
    ChatRepository chatRepository;

    @Mock
    LinkChecker linkChecker;

    @InjectMocks
    LinkService linkService;

    @BeforeEach
    void setUp() {
        when(chatRepository.existsChat(1L)).thenReturn(true);
    }

    @Test
    void getLinks_returns_all_links_when_no_tag() {
        var link = new TrackedLink(1L, GITHUB_URL, List.of("work"), List.of(), Instant.now());
        when(linkRepository.findByChat(1L)).thenReturn(List.of(link));

        List<LinkResponse> result = linkService.getLinks(1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().url()).isEqualTo(GITHUB_URL);
    }

    @Test
    void getLinks_filters_by_tag() {
        var linkWork = new TrackedLink(1L, GITHUB_URL, List.of("work"), List.of(), Instant.now());
        var linkHobby =
                new TrackedLink(2L, URI.create("https://github.com/a/b"), List.of("hobby"), List.of(), Instant.now());
        when(linkRepository.findByChat(1L)).thenReturn(List.of(linkWork, linkHobby));

        List<LinkResponse> result = linkService.getLinks(1L, List.of("work"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().url()).isEqualTo(GITHUB_URL);
    }

    @Test
    void addLink_throws_when_url_not_supported() {
        when(linkChecker.supports(UNSUPPORTED_URL)).thenReturn(false);

        assertThatThrownBy(() -> linkService.addLink(1L, UNSUPPORTED_URL, List.of(), List.of()))
                .isInstanceOf(UnsupportedLinkException.class);
    }

    @Test
    void addLink_saves_link_when_url_is_supported() {
        var saved = new TrackedLink(1L, GITHUB_URL, List.of(), List.of(), Instant.now());
        when(linkChecker.supports(GITHUB_URL)).thenReturn(true);
        when(linkRepository.addLink(1L, GITHUB_URL, List.of(), List.of())).thenReturn(saved);

        LinkResponse result = linkService.addLink(1L, GITHUB_URL, List.of(), List.of());

        assertThat(result.url()).isEqualTo(GITHUB_URL);
        verify(linkRepository).addLink(1L, GITHUB_URL, List.of(), List.of());
    }

    @Test
    void removeLink_delegates_to_repository() {
        var removed = new TrackedLink(1L, GITHUB_URL, List.of(), List.of(), Instant.now());
        when(linkRepository.removeLink(1L, GITHUB_URL)).thenReturn(removed);

        LinkResponse result = linkService.removeLink(1L, GITHUB_URL);

        assertThat(result.url()).isEqualTo(GITHUB_URL);
        verify(linkRepository).removeLink(1L, GITHUB_URL);
    }
}
