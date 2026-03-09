package backend.academy.linktracker.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
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
class ListCommandIntegrationTest {

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
    void list_shows_all_tracked_links() {
        stubGetUpdates("list-all", "/list");
        stubFor(get(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            [
                              {"id":1,"url":"https://github.com/foo/bar","tags":[],"filters":[]},
                              {"id":2,"url":"https://stackoverflow.com/questions/12345","tags":[],"filters":[]}
                            ]
                            """)));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages();

        String expected = "https://github.com/foo/bar\nhttps://stackoverflow.com/questions/12345";
        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody(expected))));
    }

    @Test
    void list_shows_empty_message_when_no_links() {
        stubGetUpdates("list-empty", "/list");
        stubFor(get(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages();

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Нет отслеживаемых ссылок."))));
    }

    @Test
    void list_with_tag_passes_tag_to_scrapper_and_shows_filtered_links() {
        stubGetUpdates("list-tag", "/list тег1");
        stubFor(get(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                            [{"id":1,"url":"https://github.com/foo/bar","tags":["тег1"],"filters":[]}]
                            """)));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages();

        verify(1, getRequestedFor(urlPathEqualTo("/links")).withQueryParam("tag", equalTo("тег1")));
        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("https://github.com/foo/bar"))));
    }

    @Test
    void list_auto_registers_chat_on_404_and_shows_result() {
        stubGetUpdates("list-auto-reg", "/list");
        stubFor(get(urlPathEqualTo("/links"))
                .inScenario("links-auto-reg")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                        aResponse()
                                .withStatus(404)
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                .withBody(
                                        "{\"description\":\"Not found\",\"code\":\"404\",\"exceptionName\":\"\",\"exceptionMessage\":\"\",\"stacktrace\":[]}"))
                .willSetStateTo("registered"));
        stubFor(post(urlPathMatching("/tg-chat/\\d+")).willReturn(aResponse().withStatus(200)));
        stubFor(get(urlPathEqualTo("/links"))
                .inScenario("links-auto-reg")
                .whenScenarioStateIs("registered")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("[]")));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages();

        verify(1, postRequestedFor(urlPathMatching("/tg-chat/\\d+")));
        verify(2, getRequestedFor(urlPathEqualTo("/links")));
        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Нет отслеживаемых ссылок."))));
    }

    private void stubGetUpdates(String scenario, String message) {
        stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(buildUpdateBody(message)))
                .willSetStateTo(scenario + "-done"));
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

    private void awaitSendMessages() {
        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(1, postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))));
    }

    private String expectedBody(String text) {
        return "chat_id=" + CHAT_ID + "&text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String buildUpdateBody(String text) {
        return """
            {
              "ok": true,
              "result": [
                {
                  "update_id": 1,
                  "message": {
                    "message_id": 1,
                    "from": {"id": %d, "is_bot": false, "first_name": "User"},
                    "chat": {"id": %d, "type": "private"},
                    "date": 1234567890,
                    "text": "%s"
                  }
                }
              ]
            }
            """.formatted(CHAT_ID, CHAT_ID, text);
    }
}
