package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GithubClient;
import backend.academy.linktracker.scrapper.configuration.ScrapperClientConfiguration;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class HttpTimeoutTest {

    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);
    private static final int SERVER_DELAY_MS = 2_000;

    private WireMockServer wiremock;
    private GithubClient client;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();

        var restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wiremock.port())
                .requestFactory(ScrapperClientConfiguration.requestFactory(Duration.ofSeconds(1), READ_TIMEOUT))
                .build();
        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        client = factory.createClient(GithubClient.class);
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    @Test
    void request_fails_with_timeout_when_server_responds_slower_than_read_timeout() {
        wiremock.stubFor(get(urlPathMatching("/repos/.*"))
                .willReturn(aResponse().withStatus(200).withFixedDelay(SERVER_DELAY_MS)));

        long start = System.nanoTime();

        assertThatThrownBy(() -> client.getRepository("foo", "bar")).isInstanceOf(ResourceAccessException.class);

        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(elapsed).isLessThan(Duration.ofMillis(SERVER_DELAY_MS));
        assertThat(elapsed).isGreaterThanOrEqualTo(READ_TIMEOUT);
    }
}
