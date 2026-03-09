package backend.academy.linktracker.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.bot.listener.BotUpdateListener;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.TelegramBot;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class TrackCommandIntegrationTest {

    static final long CHAT_ID = 987654321L;

    @Autowired
    TelegramBot telegramBot;

    @Autowired
    BotUpdateListener botUpdateListener;

    @Autowired
    ChatStateService chatStateService;

    @BeforeEach
    void setUp() {
        resetAllRequests();
        stubSendMessage();
    }

    @AfterEach
    void tearDown() {
        telegramBot.removeGetUpdatesListener();
        chatStateService.clearState(CHAT_ID);
        resetAllRequests();
    }

    @Test
    void track_full_happy_path_adds_link() {
        stubGetUpdates("track-happy", "/track", "https://github.com/foo/bar", "тег1");
        stubFor(post(urlPathEqualTo("/links")).willReturn(aResponse().withStatus(200)));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(3);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка добавлена."))));
    }

    @Test
    void track_invalid_url_shows_error_message() {
        stubGetUpdates("track-invalid", "/track", "не ссылка");

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(2);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Некорректная ссылка. Введите ссылку ещё раз:"))));
    }

    @Test
    void track_already_tracked_link_shows_error_message() {
        stubGetUpdates("track-409", "/track", "https://github.com/foo/bar", "тег1");
        stubFor(post(urlPathEqualTo("/links")).willReturn(aResponse().withStatus(409)));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(3);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка уже отслеживается."))));
    }

    @Test
    void track_unsupported_link_shows_error_message() {
        stubGetUpdates("track-422", "/track", "https://github.com/foo/bar", "тег1");
        stubFor(post(urlPathEqualTo("/links")).willReturn(aResponse().withStatus(422)));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(3);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка не поддерживается."))));
    }

    private void stubGetUpdates(String scenario, String... messages) {
        for (int i = 0; i < messages.length; i++) {
            String from = i == 0 ? STARTED : scenario + "-" + i;
            String to = i == messages.length - 1 ? scenario + "-done" : scenario + "-" + (i + 1);
            stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                    .inScenario(scenario)
                    .whenScenarioStateIs(from)
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody(buildUpdateBody(i + 1, messages[i])))
                    .willSetStateTo(to));
        }
        stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario(scenario)
                .whenScenarioStateIs(scenario + "-done")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"ok\":true,\"result\":[]}")));
    }

    private void stubSendMessage() {
        stubFor(post(urlMatching("/bot[^/]+/sendMessage"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            {
                              "ok": true,
                              "result": {
                                "message_id": 1,
                                "chat": {"id": %d, "type": "private"},
                                "date": 1234567890,
                                "text": "response"
                              }
                            }
                            """.formatted(CHAT_ID))));
    }

    private void awaitSendMessages(int count) {
        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(count, postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))));
    }

    private String expectedBody(String text) {
        return "chat_id=" + CHAT_ID + "&text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String buildUpdateBody(int updateId, String text) {
        return """
            {
              "ok": true,
              "result": [
                {
                  "update_id": %d,
                  "message": {
                    "message_id": %d,
                    "from": {"id": %d, "is_bot": false, "first_name": "User"},
                    "chat": {"id": %d, "type": "private"},
                    "date": 1234567890,
                    "text": "%s"
                  }
                }
              ]
            }
            """.formatted(updateId, updateId, CHAT_ID, CHAT_ID, text);
    }
}
