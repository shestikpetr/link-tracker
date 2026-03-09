package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    void checkLink_returns_empty_when_no_update_since_last_check() {
        Instant lastChecked = Instant.now();
        Instant apiActivity = lastChecked.minusSeconds(60); // API вернул время до lastChecked

        var link = new TrackedLink(1L, GITHUB_URL, List.of(), List.of(), lastChecked);
        when(githubHandler.supports(GITHUB_URL)).thenReturn(true);
        when(githubHandler.getLastActivity(GITHUB_URL)).thenReturn(apiActivity);

        Optional<Instant> result = linkChecker.checkLink(link);

        assertThat(result).isEmpty();
    }

    @Test
    void checkLink_returns_last_activity_when_updated_after_last_check() {
        Instant lastChecked = Instant.now().minusSeconds(60);
        Instant apiActivity = Instant.now(); // API вернул время после lastChecked

        var link = new TrackedLink(1L, GITHUB_URL, List.of(), List.of(), lastChecked);
        when(githubHandler.supports(GITHUB_URL)).thenReturn(true);
        when(githubHandler.getLastActivity(GITHUB_URL)).thenReturn(apiActivity);

        Optional<Instant> result = linkChecker.checkLink(link);

        assertThat(result).contains(apiActivity);
    }

    @Test
    void checkLink_returns_empty_when_no_handler_supports_url() {
        URI unsupported = URI.create("https://unsupported.com/foo");
        when(githubHandler.supports(unsupported)).thenReturn(false);

        var link = new TrackedLink(1L, unsupported, List.of(), List.of(), Instant.now());

        assertThat(linkChecker.checkLink(link)).isEmpty();
    }
}
