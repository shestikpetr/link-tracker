package backend.academy.linktracker.bot.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

public final class WireMockHelper {
    public static final long CHAT_ID = 987654321L;

    private WireMockHelper() {}

    public static String errorResponse(String description, int code, String exceptionName) {
        return """
                {"description":"%s","code":"%d","exceptionName":"%s","exceptionMessage":"","stacktrace":[]}""".formatted(description, code, exceptionName);
    }

    public static void stubSendMessage() {
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

    public static void stubChatRegistration() {
        stubFor(post(urlPathMatching("/tg-chat/\\d+")).willReturn(aResponse().withStatus(200)));
    }

    public static void stubGetUpdates(String scenario, String... messages) {
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

    public static void awaitSendMessages(int count) {
        await().atMost(10, SECONDS)
                .untilAsserted(() -> verify(count, postRequestedFor(urlMatching("/bot[^/]+/sendMessage"))));
    }

    public static void awaitSendMessages() {
        awaitSendMessages(1);
    }

    public static String expectedBody(String text) {
        return "chat_id=" + CHAT_ID + "&text="
                + URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String buildUpdateBody(int updateId, String text) {
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
