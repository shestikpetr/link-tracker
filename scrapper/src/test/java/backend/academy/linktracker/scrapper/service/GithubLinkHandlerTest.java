package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.service.formatter.GithubUpdateFormatter;
import backend.academy.linktracker.scrapper.utils.TextUtils;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GithubLinkHandlerTest extends AbstractWireMockTest {

    static final URI GITHUB_URL = URI.create("https://github.com/foo/bar");

    GithubLinkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GithubLinkHandler(
                createClient(GithubClient.class),
                new GithubUpdateFormatter(new TextUtils()),
                new GithubLinkExtractor());
    }

    @Test
    void supports_returns_true_for_github_url() {
        assertThat(handler.supports(GITHUB_URL)).isTrue();
    }

    @Test
    void supports_returns_false_for_non_github_url() {
        assertThat(handler.supports(URI.create("https://stackoverflow.com/questions/12345")))
                .isFalse();
    }

    @Test
    void supports_returns_false_for_github_url_without_path() {
        assertThat(handler.supports(URI.create("https://github.com"))).isFalse();
    }

    @Test
    void supports_returns_false_for_github_url_with_only_owner() {
        assertThat(handler.supports(URI.create("https://github.com/owner"))).isFalse();
    }

    @Test
    void checkUpdates_returns_new_issue() {
        Instant lastChecked = Instant.parse("2024-06-01T00:00:00Z");

        stubIssues("""
                [
                  {
                    "title": "Bug in parser",
                    "user": {"login": "alice"},
                    "created_at": "2024-06-02T10:00:00Z",
                    "body": "The parser fails on edge cases"
                  }
                ]
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(GITHUB_URL, lastChecked);

        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst().description()).contains("Issue", "Bug in parser", "alice");
        assertThat(updates.getFirst().timestamp()).isEqualTo(Instant.parse("2024-06-02T10:00:00Z"));
    }

    @Test
    void checkUpdates_returns_new_pull_request() {
        Instant lastChecked = Instant.parse("2024-06-01T00:00:00Z");

        stubIssues("""
                [
                  {
                    "title": "Fix parser",
                    "user": {"login": "bob"},
                    "created_at": "2024-06-02T12:00:00Z",
                    "body": "Fixed the edge case",
                    "pull_request": {"url": "https://api.github.com/repos/foo/bar/pulls/1"}
                  }
                ]
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(GITHUB_URL, lastChecked);

        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst().description()).contains("Pull Request", "Fix parser", "bob");
    }

    @Test
    void checkUpdates_truncates_preview_to_200_chars() {
        Instant lastChecked = Instant.parse("2024-06-01T00:00:00Z");
        String longBody = "A".repeat(300);

        stubIssues("""
                [
                  {
                    "title": "Long issue",
                    "user": {"login": "alice"},
                    "created_at": "2024-06-02T10:00:00Z",
                    "body": "%s"
                  }
                ]
                """.formatted(longBody));

        List<LinkUpdateInfo> updates = handler.checkUpdates(GITHUB_URL, lastChecked);

        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst().description()).doesNotContain("A".repeat(201));
        assertThat(updates.getFirst().description()).contains("A".repeat(200) + "...");
    }

    @Test
    void checkUpdates_returns_empty_when_no_new_issues() {
        stubIssues("[]");

        List<LinkUpdateInfo> updates = handler.checkUpdates(GITHUB_URL, Instant.parse("2024-06-01T00:00:00Z"));

        assertThat(updates).isEmpty();
    }

    @Test
    void checkUpdates_filters_out_old_issues() {
        stubIssues("""
                [
                  {
                    "title": "Old issue",
                    "user": {"login": "alice"},
                    "created_at": "2024-06-02T10:00:00Z",
                    "body": "old"
                  }
                ]
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(GITHUB_URL, Instant.parse("2024-06-03T00:00:00Z"));

        assertThat(updates).isEmpty();
    }

    @Test
    void checkUpdates_throws_on_api_error() {
        wiremock.stubFor(get(urlPathEqualTo("/repos/foo/bar/issues"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> handler.checkUpdates(GITHUB_URL, Instant.now()))
                .isInstanceOf(Exception.class);
    }

    private void stubIssues(String body) {
        wiremock.stubFor(get(urlPathEqualTo("/repos/foo/bar/issues")).willReturn(jsonResponse(body)));
    }
}
