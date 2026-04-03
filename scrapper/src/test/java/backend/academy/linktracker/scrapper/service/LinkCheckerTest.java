package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void getLastActivity_returns_activity_when_handler_supports_url() {
        Instant apiActivity = Instant.now();
        when(githubHandler.supports(GITHUB_URL)).thenReturn(true);
        when(githubHandler.getLastActivity(GITHUB_URL)).thenReturn(apiActivity);

        assertThat(linkChecker.getLastActivity(GITHUB_URL)).contains(apiActivity);
    }

    @Test
    void getLastActivity_returns_empty_when_no_handler_supports_url() {
        URI unsupported = URI.create("https://unsupported.com/foo");
        when(githubHandler.supports(unsupported)).thenReturn(false);

        assertThat(linkChecker.getLastActivity(unsupported)).isEmpty();
    }
}
