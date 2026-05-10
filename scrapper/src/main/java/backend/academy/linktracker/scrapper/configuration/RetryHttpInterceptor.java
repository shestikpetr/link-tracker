package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.exceptions.RetryableHttpStatusException;
import io.github.resilience4j.retry.Retry;
import java.io.IOException;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

@Slf4j
public class RetryHttpInterceptor implements ClientHttpRequestInterceptor {

    private final Retry retry;
    private final Set<Integer> retryableStatuses;

    public RetryHttpInterceptor(Retry retry, Set<Integer> retryableStatuses) {
        this.retry = retry;
        this.retryableStatuses = retryableStatuses;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        try {
            return Retry.decorateCheckedSupplier(retry, () -> executeOnce(request, body, execution))
                    .get();
        } catch (IOException | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    private ClientHttpResponse executeOnce(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        int status = response.getStatusCode().value();
        if (retryableStatuses.contains(status)) {
            String statusText = response.getStatusText();
            response.close();
            throw new RetryableHttpStatusException(status, statusText);
        }
        return response;
    }
}
