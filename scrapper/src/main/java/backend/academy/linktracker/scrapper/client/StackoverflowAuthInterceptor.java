package backend.academy.linktracker.scrapper.client;

import backend.academy.linktracker.scrapper.properties.StackoverflowProperties;
import java.io.IOException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
public class StackoverflowAuthInterceptor implements ClientHttpRequestInterceptor {
    private final StackoverflowProperties properties;

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        URI authorizedUri = UriComponentsBuilder.fromUri(request.getURI())
                .queryParam("key", properties.getKey())
                .queryParam("access_token", properties.getAccessToken())
                .build(true)
                .toUri();

        HttpRequest wrapped = new HttpRequestWrapper(request) {
            @Override
            public URI getURI() {
                return authorizedUri;
            }
        };
        return execution.execute(wrapped, body);
    }
}
