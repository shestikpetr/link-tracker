package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.configuration.CircuitBreakerHttpInterceptor;
import backend.academy.linktracker.scrapper.configuration.ScrapperClientConfiguration;
import backend.academy.linktracker.scrapper.exceptions.RetryableHttpStatusException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class HttpCircuitBreakerTest {

    private static final int SLIDING_WINDOW_SIZE = 5;
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final float FAILURE_RATE_THRESHOLD = 50.0f;
    private static final Duration WAIT_DURATION_IN_OPEN_STATE = Duration.ofMillis(500);
    private static final int PERMITTED_CALLS_IN_HALF_OPEN_STATE = 3;
    private static final String JSON_REPO = """
            {"id":1,"name":"bar","full_name":"foo/bar","description":"d","html_url":"http://x","stargazers_count":0}
            """;

    private WireMockServer wiremock;
    private CircuitBreaker circuitBreaker;
    private GithubClient client;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .waitDurationInOpenState(WAIT_DURATION_IN_OPEN_STATE)
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN_STATE)
                .recordExceptions(RetryableHttpStatusException.class, IOException.class)
                .build();
        circuitBreaker = CircuitBreaker.of("test-cb", config);
        var interceptor = new CircuitBreakerHttpInterceptor(circuitBreaker);

        var restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wiremock.port())
                .requestFactory(
                        ScrapperClientConfiguration.requestFactory(Duration.ofSeconds(1), Duration.ofSeconds(5)))
                .requestInterceptor(interceptor)
                .build();
        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        client = factory.createClient(GithubClient.class);
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    // 5xx стабильно -> CB переходит в OPEN, последующие вызовы отклоняются без обращения
    @Test
    void transitions_to_open_on_failures() {
        wiremock.stubFor(
                get(urlPathMatching("/repos/.*")).willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < MINIMUM_NUMBER_OF_CALLS; i++) {
            assertThatThrownBy(() -> client.getRepository("foo", "bar"));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        wiremock.verify(MINIMUM_NUMBER_OF_CALLS, getRequestedFor(urlPathMatching("/repos/foo/bar")));

        assertThatThrownBy(() -> client.getRepository("foo", "bar")).isInstanceOf(CallNotPermittedException.class);

        wiremock.verify(MINIMUM_NUMBER_OF_CALLS, getRequestedFor(urlPathMatching("/repos/foo/bar")));
    }

    // HALF-OPEN -> CLOSED при успешных пробных вызовах
    @Test
    void transitions_half_open_to_closed_on_successful_probe_calls() {
        openCircuit();

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> circuitBreaker.tryAcquirePermission()
                        || circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);

        circuitBreaker.releasePermission();

        wiremock.resetAll();
        wiremock.stubFor(get(urlPathMatching("/repos/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(JSON_REPO)));

        for (int i = 0; i < PERMITTED_CALLS_IN_HALF_OPEN_STATE; i++) {
            client.getRepository("foo", "bar");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    // HALF-OPEN -> OPEN при неуспешных пробных вызовах
    @Test
    void transitions_half_open_to_open_on_failed_probe_calls() {
        openCircuit();

        await().atMost(2, TimeUnit.SECONDS)
                .until(() -> circuitBreaker.tryAcquirePermission()
                        || circuitBreaker.getState() == CircuitBreaker.State.HALF_OPEN);
        circuitBreaker.releasePermission();

        for (int i = 0; i < PERMITTED_CALLS_IN_HALF_OPEN_STATE; i++) {
            assertThatThrownBy(() -> client.getRepository("foo", "bar"));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private void openCircuit() {
        wiremock.stubFor(get(urlPathMatching("/repos/.*"))
                .inScenario("open")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < MINIMUM_NUMBER_OF_CALLS; i++) {
            assertThatThrownBy(() -> client.getRepository("foo", "bar"));
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }
}
