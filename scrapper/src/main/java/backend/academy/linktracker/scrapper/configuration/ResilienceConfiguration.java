package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.exceptions.RetryableHttpStatusException;
import backend.academy.linktracker.scrapper.properties.RetryProperties;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ResilienceConfiguration {

    public static final String EXTERNAL_HTTP_RETRY = "external-http";

    @Bean
    public Retry externalHttpRetry(RetryProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.getMaxAttempts())
                .intervalFunction(IntervalFunction.of(properties.getWaitDuration()))
                .retryExceptions(RetryableHttpStatusException.class, IOException.class)
                .build();

        Retry retry = Retry.of(EXTERNAL_HTTP_RETRY, config);

        retry.getEventPublisher()
                .onRetry(event -> log.atWarn()
                        .setMessage("Повторная попытка HTTP запроса")
                        .addKeyValue("attempt", event.getNumberOfRetryAttempts())
                        .addKeyValue("error", event.getLastThrowable().getMessage())
                        .log())
                .onError(event -> log.atError()
                        .setMessage("Все попытки HTTP запроса исчерпаны")
                        .addKeyValue("attempts", event.getNumberOfRetryAttempts())
                        .addKeyValue("error", event.getLastThrowable().getMessage())
                        .log());

        return retry;
    }

    @Bean
    public RetryHttpInterceptor retryHttpInterceptor(Retry externalHttpRetry, RetryProperties properties) {
        return new RetryHttpInterceptor(externalHttpRetry, properties.getRetryableStatuses());
    }
}
