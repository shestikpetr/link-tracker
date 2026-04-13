package backend.academy.linktracker.scrapper.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;

abstract class AbstractWireMockTest {

    WireMockServer wiremock;

    @BeforeEach
    void setUpWireMock() {
        wiremock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wiremock.start();
    }

    @AfterEach
    void tearDownWireMock() {
        wiremock.stop();
    }

    <T> T createClient(Class<T> clientClass) {
        var uriFactory = new DefaultUriBuilderFactory("http://localhost:" + wiremock.port());
        uriFactory.setDefaultUriVariables(Map.of("key", "test-key", "access_token", "test-token"));

        var restClient = RestClient.builder().uriBuilderFactory(uriFactory).build();
        var factory = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build();
        return factory.createClient(clientClass);
    }

    static ResponseDefinitionBuilder jsonResponse(String body) {
        return aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody(body);
    }
}
