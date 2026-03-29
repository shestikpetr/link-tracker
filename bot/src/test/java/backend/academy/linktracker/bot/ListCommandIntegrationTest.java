package backend.academy.linktracker.bot;

import static backend.academy.linktracker.bot.utils.WireMockHelper.CHAT_ID;
import static backend.academy.linktracker.bot.utils.WireMockHelper.awaitSendMessages;
import static backend.academy.linktracker.bot.utils.WireMockHelper.expectedBody;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubChatRegistration;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubGetUpdates;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubSendMessage;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import backend.academy.linktracker.bot.listener.BotUpdateListener;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.TelegramBot;
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
        stubChatRegistration();
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
        stubLinksResponse("""
                [
                  {"id":1,"url":"https://github.com/foo/bar","tags":[],"filters":[]},
                  {"id":2,"url":"https://stackoverflow.com/questions/12345","tags":[],"filters":[]}
                ]
                """);

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
        stubLinksResponse("[]");

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
        stubLinksResponse("""
                [{"id":1,"url":"https://github.com/foo/bar","tags":["тег1"],"filters":[]}]
                """);

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages();

        verify(1, getRequestedFor(urlPathEqualTo("/links")).withQueryParam("tags", equalTo("тег1")));
        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("https://github.com/foo/bar"))));
    }

    @Test
    void list_registers_chat_before_fetching_links() {
        stubGetUpdates("list-auto-reg", "/list");
        stubLinksResponse("[]");

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages();

        verify(1, postRequestedFor(urlPathMatching("/tg-chat/\\d+")));
        verify(1, getRequestedFor(urlPathEqualTo("/links")));
        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Нет отслеживаемых ссылок."))));
    }

    private void stubLinksResponse(String body) {
        stubFor(get(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(body)));
    }
}
