package backend.academy.linktracker.bot;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathTemplate;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.listener.BotUpdateListener;
import backend.academy.linktracker.bot.properties.TelegramProperties;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.pengrad.telegrambot.TelegramBot;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
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
class BotCommandsIntegrationTest {

    private static final long CHAT_ID = 987654321L;

    @Autowired
    TelegramBot telegramBot;

    @Autowired
    BotUpdateListener botUpdateListener;

    @Autowired
    TelegramProperties telegramProperties;

    @Autowired
    CommandRegistry commandRegistry;

    @BeforeEach
    void setUp() {
        stubSendMessage();
        telegramBot.setUpdatesListener(botUpdateListener);
    }

    @AfterEach
    void clearUpdatesListener() {
        telegramBot.removeGetUpdatesListener();
        resetAllRequests();
    }

    @Test
    void start_command_sends_welcome_message() {
        String text = "Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды.";
        executeCommand("/start");

        verify(sendMessageRequest().withRequestBody(equalTo(expectedBody(text))));
    }

    @Test
    void help_command_lists_all_commands() {
        String expectedText = commandRegistry.getAll().stream()
                .map(cmd -> cmd.command() + " - " + cmd.description())
                .collect(Collectors.joining("\n"));

        executeCommand("/help");

        verify(sendMessageRequest().withRequestBody(equalTo(expectedBody(expectedText))));
    }

    @Test
    void unknown_command_sends_help_hint() {
        String text = "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд.";
        executeCommand("/unknown");

        verify(sendMessageRequest().withRequestBody(equalTo(expectedBody(text))));
    }

    private void executeCommand(String command) {
        stubGetUpdatesWithCommand(command);
        awaitSendMessage();
    }

    private void stubGetUpdatesWithCommand(String command) {
        String scenario = "command:" + command;
        stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(buildUpdateBody(command)))
                .willSetStateTo("done"));

        stubFor(post(urlMatching("/bot[^/]+/getUpdates"))
                .inScenario(scenario)
                .whenScenarioStateIs("done")
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

    private RequestPatternBuilder sendMessageRequest() {
        return postRequestedFor(urlPathTemplate("/bot{token}/sendMessage"))
                .withPathParam("token", equalTo(telegramProperties.getToken()));
    }

    private void awaitSendMessage() {
        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(1, postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))));
    }

    private String expectedBody(String text) {
        return "chat_id=" + CHAT_ID + "&text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String buildUpdateBody(String command) {
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
            """.formatted(CHAT_ID, CHAT_ID, command);
    }
}
