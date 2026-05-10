package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.configuration.RetryHttpInterceptor;
import backend.academy.linktracker.scrapper.configuration.ScrapperClientConfiguration;
import backend.academy.linktracker.scrapper.exceptions.RetryableHttpStatusException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class HttpRetryTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration WAIT_DURATION = Duration.ofMillis(200);
    private static final Set<Integer> RETRYABLE_STATUSES = Set.of(500, 502, 503, 504);
    private static final String JSON_REPO = """
        {"id":1,"name":"bar","full_name":"foo/bar","description":"d","html_url":"http://x","stargazers_count":0}
        """;

    private WireMockServer wiremock;
    private GithubClient client;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
        client = buildClient(IntervalFunction.of(WAIT_DURATION));
    }

    private GithubClient buildClient(IntervalFunction intervalFunction) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(MAX_ATTEMPTS)
                .intervalFunction(intervalFunction)
                .retryExceptions(RetryableHttpStatusException.class, IOException.class)
                .build();
        Retry retry = Retry.of("test-retry-" + System.nanoTime(), config);
        var interceptor = new RetryHttpInterceptor(retry, RETRYABLE_STATUSES);

        var restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wiremock.port())
                .requestFactory(
                        ScrapperClientConfiguration.requestFactory(Duration.ofSeconds(1), Duration.ofSeconds(5)))
                .requestInterceptor(interceptor)
                .build();
        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(GithubClient.class);
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    // Retry на 5xx - последовательность 500, 500, 200 -> итоговый ответ успешный, 3 запроса
    @Test
    void retries_on_5xx_until_success() {
        wiremock.stubFor(get(urlPathMatching("/repos/.*"))
                .inScenario("retry-5xx")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-2"));
        wiremock.stubFor(get(urlPathMatching("/repos/.*"))
                .inScenario("retry-5xx")
                .whenScenarioStateIs("attempt-2")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-3"));
        wiremock.stubFor(get(urlPathMatching("/repos/.*"))
                .inScenario("retry-5xx")
                .whenScenarioStateIs("attempt-3")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(JSON_REPO)));

        var response = client.getRepository("foo", "bar");

        assertThat(response).isNotNull();
        wiremock.verify(MAX_ATTEMPTS, getRequestedFor(urlPathMatching("/repos/foo/bar")));
    }

    // Retry не выполняется на 4xx - статус 400, ровно 1 запрос
    @Test
    void does_not_retry_on_4xx() {
        wiremock.stubFor(
                get(urlPathMatching("/repos/.*")).willReturn(aResponse().withStatus(400)));

        assertThatThrownBy(() -> client.getRepository("foo", "bar")).isInstanceOf(HttpClientErrorException.class);

        wiremock.verify(1, getRequestedFor(urlPathMatching("/repos/foo/bar")));
    }

    // Constant backoff - интервалы между попытками
    @Test
    void uses_constant_backoff_between_retries() {
        wiremock.stubFor(
                get(urlPathMatching("/repos/.*")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.getRepository("foo", "bar"));

        var events = wiremock.getAllServeEvents();
        assertThat(events).hasSize(MAX_ATTEMPTS);

        // wiremock возвращает события в обратном порядке
        // новые первыми
        List<Long> timestamps = events.stream()
                .map(e -> e.getRequest().getLoggedDate().getTime())
                .sorted()
                .toList();

        long tolerance = 150L;
        for (int i = 1; i < timestamps.size(); i++) {
            long delta = timestamps.get(i) - timestamps.get(i - 1);
            assertThat(delta)
                    .as("Интервал между попытками %d и %d", i - 1, i)
                    .isGreaterThanOrEqualTo(WAIT_DURATION.toMillis() - tolerance)
                    .isLessThan(WAIT_DURATION.toMillis() + 1_000);
        }
    }

    // exponential backoff - интервалы растут как initial * multiplier ** attempt
    @Test
    void uses_exponential_backoff_when_enabled() {
        double multiplier = 2.0;
        client = buildClient(IntervalFunction.ofExponentialBackoff(WAIT_DURATION, multiplier));

        wiremock.stubFor(
                get(urlPathMatching("/repos/.*")).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.getRepository("foo", "bar"));

        var events = wiremock.getAllServeEvents();
        assertThat(events).hasSize(MAX_ATTEMPTS);

        List<Long> timestamps = events.stream()
                .map(e -> e.getRequest().getLoggedDate().getTime())
                .sorted()
                .toList();

        long firstInterval = timestamps.get(1) - timestamps.get(0);
        long secondInterval = timestamps.get(2) - timestamps.get(1);

        long tolerance = 150L;
        long expectedFirst = WAIT_DURATION.toMillis();
        long expectedSecond = (long) (WAIT_DURATION.toMillis() * multiplier);

        assertThat(firstInterval)
                .as("Первый интервал ≈ initial")
                .isGreaterThanOrEqualTo(expectedFirst - tolerance)
                .isLessThan(expectedFirst + 1_000);

        assertThat(secondInterval)
                .as("Второй интервал ≈ initial * multiplier")
                .isGreaterThanOrEqualTo(expectedSecond - tolerance)
                .isLessThan(expectedSecond + 1_000);

        assertThat(secondInterval).as("Второй интервал больше первого").isGreaterThan(firstInterval);
    }
}
