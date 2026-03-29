package backend.academy.linktracker.bot;

import static backend.academy.linktracker.bot.utils.WireMockHelper.CHAT_ID;
import static backend.academy.linktracker.bot.utils.WireMockHelper.awaitSendMessages;
import static backend.academy.linktracker.bot.utils.WireMockHelper.errorResponse;
import static backend.academy.linktracker.bot.utils.WireMockHelper.expectedBody;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubGetUpdates;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubSendMessage;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
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
class UntrackCommandIntegrationTest {

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
    void untrack_happy_path_removes_link() {
        stubGetUpdates("untrack-happy", "/untrack", "https://github.com/foo/bar");
        stubFor(delete(urlPathEqualTo("/links")).willReturn(aResponse().withStatus(200)));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(2);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка удалена."))));
    }

    @Test
    void untrack_link_not_found_shows_error_message() {
        stubGetUpdates("untrack-404", "/untrack", "https://github.com/foo/bar");
        stubFor(delete(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(errorResponse("Ссылка не найдена", 404, "LinkNotFoundException"))));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(2);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка не найдена"))));
    }
}
