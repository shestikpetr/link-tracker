package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
class ResilientNotificationSenderTest {

    private static final URI URL = URI.create("https://github.com/foo/bar");
    private static final String DESCRIPTION = "new commit";
    private static final List<Long> CHAT_IDS = List.of(1L, 2L);

    @Mock
    HttpNotificationSender httpSender;

    @Mock
    KafkaNotificationSender kafkaSender;

    @InjectMocks
    ResilientNotificationSender sender;

    @Test
    void uses_http_when_available() {
        sender.send(URL, DESCRIPTION, CHAT_IDS);

        verify(httpSender, times(1)).send(eq(URL), eq(DESCRIPTION), eq(CHAT_IDS));
        verify(kafkaSender, never()).send(any(), any(), any());
    }

    // HTTP падает -> Kafka
    @Test
    void falls_back_to_kafka_when_http_throws_resource_access_exception() {
        doThrow(new ResourceAccessException("connection refused"))
                .when(httpSender)
                .send(eq(URL), eq(DESCRIPTION), eq(CHAT_IDS));

        sender.send(URL, DESCRIPTION, CHAT_IDS);

        verify(httpSender, times(1)).send(eq(URL), eq(DESCRIPTION), eq(CHAT_IDS));
        verify(kafkaSender, times(1)).send(eq(URL), eq(DESCRIPTION), eq(CHAT_IDS));
    }

    @Test
    void falls_back_to_kafka_when_circuit_breaker_open() {
        CircuitBreaker openCb = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        openCb.transitionToOpenState();
        Throwable cbException = CallNotPermittedException.createCallNotPermittedException(openCb);

        doThrow(cbException).when(httpSender).send(eq(URL), eq(DESCRIPTION), eq(CHAT_IDS));

        sender.send(URL, DESCRIPTION, CHAT_IDS);

        verify(kafkaSender, times(1)).send(eq(URL), eq(DESCRIPTION), eq(CHAT_IDS));
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
