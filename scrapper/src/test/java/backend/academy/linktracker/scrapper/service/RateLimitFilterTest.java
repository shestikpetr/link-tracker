package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.configuration.RateLimitFilter;
import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

class RateLimitFilterTest {

    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);
    private static final String IP = "10.0.0.1";

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setCapacity(CAPACITY);
        properties.setRefillPeriod(REFILL_PERIOD);
        filter = new RateLimitFilter(properties);
    }

    // Запросы в пределах лимита проходят, превышающие получают 429
    @Test
    void allows_requests_within_limit_and_rejects_excess_with_429() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < CAPACITY; i++) {
            HttpServletResponse response = mock(HttpServletResponse.class);
            filter.doFilter(requestFromIp(IP), response, chain);
            verify(response, never()).setStatus(anyInt());
        }
        verify(chain, times(CAPACITY)).doFilter(any(), any());

        HttpServletResponse rejected = mock(HttpServletResponse.class);
        filter.doFilter(requestFromIp(IP), rejected, chain);

        verify(rejected).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(rejected).setHeader(eq(HttpHeaders.RETRY_AFTER), anyString());
        verify(chain, times(CAPACITY)).doFilter(any(), any());
    }

    @Test
    void limits_per_ip_independently() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < CAPACITY; i++) {
            filter.doFilter(requestFromIp("10.0.0.1"), mock(HttpServletResponse.class), chain);
        }

        HttpServletResponse otherIpResponse = mock(HttpServletResponse.class);
        filter.doFilter(requestFromIp("10.0.0.2"), otherIpResponse, chain);

        verify(otherIpResponse, never()).setStatus(anyInt());
        verify(chain, atLeastOnce()).doFilter(any(), any());
    }

    private static HttpServletRequest requestFromIp(String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(ip);
        when(request.getRequestURI()).thenReturn("/links");
        return request;
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }

    private static String eq(String value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
