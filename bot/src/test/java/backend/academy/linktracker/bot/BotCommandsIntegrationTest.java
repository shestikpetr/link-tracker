package backend.academy.linktracker.bot;

import static backend.academy.linktracker.bot.utils.WireMockHelper.awaitSendMessages;
import static backend.academy.linktracker.bot.utils.WireMockHelper.expectedBody;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubGetUpdates;
import static backend.academy.linktracker.bot.utils.WireMockHelper.stubSendMessage;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.listener.BotUpdateListener;
import com.pengrad.telegrambot.TelegramBot;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@EnableWireMock
class BotCommandsIntegrationTest {

    @Autowired
    TelegramBot telegramBot;

    @Autowired
    BotUpdateListener botUpdateListener;

    @Autowired
    CommandRegistry commandRegistry;

    @BeforeEach
    void setUp() {
        stubSendMessage();
        telegramBot.setUpdatesListener(botUpdateListener);
    }

    @AfterEach
    void tearDown() {
        telegramBot.removeGetUpdatesListener();
        resetAllRequests();
    }

    @Test
    void start_command_sends_welcome_message() {
        stubGetUpdates("start", "/start");
        awaitSendMessages();

        verify(postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                .withRequestBody(equalTo(
                        expectedBody("Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды."))));
    }

    @Test
    void help_command_lists_all_commands() {
        String expectedText = commandRegistry.getAll().stream()
                .map(cmd -> cmd.command() + " - " + cmd.description())
                .collect(Collectors.joining("\n"));

        stubGetUpdates("help", "/help");
        awaitSendMessages();

        verify(postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                .withRequestBody(equalTo(expectedBody(expectedText))));
    }

    @Test
    void unknown_command_sends_help_hint() {
        stubGetUpdates("unknown", "/unknown");
        awaitSendMessages();

        verify(postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))
                .withRequestBody(equalTo(expectedBody(
                        "Неизвестная команда. Воспользуйтесь /help, чтобы посмотреть список доступных команд."))));
    }
}
