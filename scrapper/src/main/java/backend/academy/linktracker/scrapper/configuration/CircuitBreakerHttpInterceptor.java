package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.exceptions.RetryableHttpStatusException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class CircuitBreakerHttpInterceptor implements ClientHttpRequestInterceptor {

    private final CircuitBreaker circuitBreaker;

    public CircuitBreakerHttpInterceptor(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        try {
            return CircuitBreaker.decorateCheckedSupplier(
                            circuitBreaker, () -> executeAndCheck(request, body, execution))
                    .get();
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    private ClientHttpResponse executeAndCheck(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode().is5xxServerError()) {
            int status = response.getStatusCode().value();
            String statusText = response.getStatusText();
            response.close();
            throw new RetryableHttpStatusException(status, statusText);
        }
        return response;
    }
}
