package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import backend.academy.linktracker.scrapper.client.GithubClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

class GithubLinkHandlerTest {

    WireMockServer wiremock;
    GithubLinkHandler handler;

    @BeforeEach
    void setUp() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();

        var restClient = RestClient.builder()
                .baseUrl("http://localhost:" + wiremock.port())
                .build();
        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        GithubClient client = factory.createClient(GithubClient.class);

        handler = new GithubLinkHandler(client);
    }

    @AfterEach
    void tearDown() {
        wiremock.stop();
    }

    @Test
    void supports_returns_true_for_github_url() {
        assertThat(handler.supports(URI.create("https://github.com/foo/bar"))).isTrue();
    }

    @Test
    void supports_returns_false_for_non_github_url() {
        assertThat(handler.supports(URI.create("https://stackoverflow.com/questions/12345")))
                .isFalse();
    }

    @Test
    void getLastActivity_returns_pushed_at_from_api() {
        wiremock.stubFor(get(urlPathEqualTo("/repos/foo/bar"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {"pushed_at": "2024-06-01T12:00:00Z"}
                                """)));

        Instant result = handler.getLastActivity(URI.create("https://github.com/foo/bar"));

        assertThat(result).isEqualTo(Instant.parse("2024-06-01T12:00:00Z"));
    }

    @Test
    void getLastActivity_throws_on_api_error() {
        wiremock.stubFor(
                get(urlPathEqualTo("/repos/foo/bar")).willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> handler.getLastActivity(URI.create("https://github.com/foo/bar")))
                .isInstanceOf(Exception.class);
    }
}
