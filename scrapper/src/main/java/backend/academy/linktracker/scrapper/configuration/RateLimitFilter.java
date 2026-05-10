package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.RateLimitProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterRegistry registry;
    private final RateLimiterConfig config;
    private final long retryAfterSeconds;

    public RateLimitFilter(RateLimitProperties properties) {
        this.config = RateLimiterConfig.custom()
                .limitForPeriod(properties.getCapacity())
                .limitRefreshPeriod(properties.getRefillPeriod())
                .timeoutDuration(java.time.Duration.ZERO)
                .build();
        this.registry = RateLimiterRegistry.of(config);
        this.retryAfterSeconds = Math.max(1, properties.getRefillPeriod().toSeconds());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        RateLimiter limiter = registry.rateLimiter("ip:" + clientIp, config);

        if (!limiter.acquirePermission()) {
            log.atWarn()
                    .setMessage("Превышен лимит запросов")
                    .addKeyValue("ip", clientIp)
                    .addKeyValue("path", request.getRequestURI())
                    .log();
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
