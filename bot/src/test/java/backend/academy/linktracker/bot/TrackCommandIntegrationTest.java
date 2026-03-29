package backend.academy.linktracker.bot;

import static backend.academy.linktracker.bot.utils.WireMockHelper.CHAT_ID;
import static backend.academy.linktracker.bot.utils.WireMockHelper.awaitSendMessages;
import static backend.academy.linktracker.bot.utils.WireMockHelper.errorResponse;
import static backend.academy.linktracker.bot.utils.WireMockHelper.expectedBody;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubChatRegistration;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubGetUpdates;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubSendMessage;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
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
class TrackCommandIntegrationTest {

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
        stubFor(post(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(errorResponse("Ссылка уже отслеживается", 409, "LinkAlreadyExistsException"))));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(3);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка уже отслеживается"))));
    }

    @Test
    void track_unsupported_link_shows_error_message() {
        stubGetUpdates("track-422", "/track", "https://github.com/foo/bar", "тег1");
        stubFor(post(urlPathEqualTo("/links"))
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(errorResponse("Ссылка не поддерживается", 422, "UnsupportedLinkException"))));

        telegramBot.setUpdatesListener(botUpdateListener);
        awaitSendMessages(3);

        verify(
                1,
                postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                        .withRequestBody(equalTo(expectedBody("Ссылка не поддерживается"))));
    }
}
