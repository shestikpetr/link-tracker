package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.StackoverflowClient;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StackoverflowLinkHandlerTest extends AbstractWireMockTest {

    static final URI SO_URL = URI.create("https://stackoverflow.com/questions/12345");

    StackoverflowLinkHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StackoverflowLinkHandler(createClient(StackoverflowClient.class));
    }

    @Test
    void supports_returns_true_for_stackoverflow_url() {
        assertThat(handler.supports(SO_URL)).isTrue();
    }

    @Test
    void supports_returns_false_for_non_stackoverflow_url() {
        assertThat(handler.supports(URI.create("https://github.com/foo/bar"))).isFalse();
    }

    @Test
    void supports_returns_false_for_stackoverflow_url_without_path() {
        assertThat(handler.supports(URI.create("https://stackoverflow.com"))).isFalse();
    }

    @Test
    void supports_returns_false_for_stackoverflow_url_without_question_id() {
        assertThat(handler.supports(URI.create("https://stackoverflow.com/questions")))
                .isFalse();
    }

    @Test
    void supports_returns_false_for_stackoverflow_url_with_non_numeric_id() {
        assertThat(handler.supports(URI.create("https://stackoverflow.com/questions/abc")))
                .isFalse();
    }

    @Test
    void checkUpdates_returns_new_answer() {
        Instant lastChecked = Instant.ofEpochSecond(1700000000);

        stubQuestion("How to parse JSON?");
        stubAnswers("""
                {"items": [
                  {
                    "owner": {"display_name": "alice"},
                    "creation_date": 1700001000,
                    "body": "Use Jackson library for parsing"
                  }
                ]}
                """);
        stubComments("""
                {"items": []}
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(SO_URL, lastChecked);

        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst().description())
                .contains("Новый ответ", "How to parse JSON?", "alice", "Use Jackson library for parsing");
    }

    @Test
    void checkUpdates_returns_new_comment() {
        Instant lastChecked = Instant.ofEpochSecond(1700000000);

        stubQuestion("How to parse JSON?");
        stubAnswers("""
                {"items": []}
                """);
        stubComments("""
                {"items": [
                  {
                    "owner": {"display_name": "bob"},
                    "creation_date": 1700001000,
                    "body": "Could you provide more details?"
                  }
                ]}
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(SO_URL, lastChecked);

        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst().description()).contains("Новый комментарий", "bob");
    }

    @Test
    void checkUpdates_truncates_preview_to_200_chars() {
        Instant lastChecked = Instant.ofEpochSecond(1700000000);
        String longBody = "B".repeat(300);

        stubQuestion("Test question");
        stubAnswers("""
                {"items": [
                  {
                    "owner": {"display_name": "alice"},
                    "creation_date": 1700001000,
                    "body": "%s"
                  }
                ]}
                """.formatted(longBody));
        stubComments("""
                {"items": []}
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(SO_URL, lastChecked);

        assertThat(updates).hasSize(1);
        assertThat(updates.getFirst().description()).doesNotContain("B".repeat(201));
        assertThat(updates.getFirst().description()).contains("B".repeat(200) + "...");
    }

    @Test
    void checkUpdates_returns_empty_when_no_new_activity() {
        stubQuestion("Test question");
        stubAnswers("""
                {"items": [
                  {
                    "owner": {"display_name": "alice"},
                    "creation_date": 1700001000,
                    "body": "old answer"
                  }
                ]}
                """);
        stubComments("""
                {"items": []}
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(SO_URL, Instant.ofEpochSecond(1700002000));

        assertThat(updates).isEmpty();
    }

    @Test
    void checkUpdates_throws_on_api_error() {
        wiremock.stubFor(get(urlPathEqualTo("/2.3/questions/12345"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> handler.checkUpdates(SO_URL, Instant.now())).isInstanceOf(Exception.class);
    }

    @Test
    void checkUpdates_returns_both_answers_and_comments() {
        Instant lastChecked = Instant.ofEpochSecond(1700000000);

        stubQuestion("Test question");
        stubAnswers("""
                {"items": [
                  {
                    "owner": {"display_name": "alice"},
                    "creation_date": 1700001000,
                    "body": "answer text"
                  }
                ]}
                """);
        stubComments("""
                {"items": [
                  {
                    "owner": {"display_name": "bob"},
                    "creation_date": 1700001500,
                    "body": "comment text"
                  }
                ]}
                """);

        List<LinkUpdateInfo> updates = handler.checkUpdates(SO_URL, lastChecked);

        assertThat(updates).hasSize(2);
    }

    private void stubQuestion(String title) {
        wiremock.stubFor(get(urlPathEqualTo("/2.3/questions/12345")).willReturn(jsonResponse("""
                        {"items": [{"title": "%s", "last_activity_date": 1700000000}]}
                        """.formatted(title))));
    }

    private void stubAnswers(String body) {
        wiremock.stubFor(get(urlPathEqualTo("/2.3/questions/12345/answers")).willReturn(jsonResponse(body)));
    }

    private void stubComments(String body) {
        wiremock.stubFor(get(urlPathEqualTo("/2.3/questions/12345/comments")).willReturn(jsonResponse(body)));
    }
}
