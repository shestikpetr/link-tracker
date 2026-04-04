package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkCheckerTest {

    static final URI GITHUB_URL = URI.create("https://github.com/foo/bar");

    LinkHandler githubHandler = mock(LinkHandler.class);
    LinkChecker linkChecker;

    @BeforeEach
    void setUp() {
        linkChecker = new LinkChecker(List.of(githubHandler));
    }

    @Test
    void supports_returns_true_when_handler_supports_url() {
        when(githubHandler.supports(GITHUB_URL)).thenReturn(true);

        assertThat(linkChecker.supports(GITHUB_URL)).isTrue();
    }

    @Test
    void supports_returns_false_when_no_handler_supports_url() {
        when(githubHandler.supports(GITHUB_URL)).thenReturn(false);

        assertThat(linkChecker.supports(GITHUB_URL)).isFalse();
    }

    @Test
    void checkUpdates_returns_updates_when_handler_supports_url() {
        Instant lastChecked = Instant.now().minusSeconds(60);
        var update = new LinkUpdateInfo(Instant.now(), "test update");
        when(githubHandler.supports(GITHUB_URL)).thenReturn(true);
        when(githubHandler.checkUpdates(GITHUB_URL, lastChecked)).thenReturn(List.of(update));

        assertThat(linkChecker.checkUpdates(GITHUB_URL, lastChecked)).containsExactly(update);
    }

    @Test
    void checkUpdates_returns_empty_when_no_handler_supports_url() {
        URI unsupported = URI.create("https://unsupported.com/foo");
        when(githubHandler.supports(unsupported)).thenReturn(false);

        assertThat(linkChecker.checkUpdates(unsupported, Instant.now())).isEmpty();
    }
}
