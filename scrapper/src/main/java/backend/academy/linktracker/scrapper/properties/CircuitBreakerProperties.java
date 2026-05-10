package backend.academy.linktracker.scrapper.properties;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@ConfigurationProperties(prefix = "app.resilience.circuit-breaker")
public class CircuitBreakerProperties {

    @NotNull
    private SlidingWindowType slidingWindowType = SlidingWindowType.COUNT_BASED;

    @Min(1)
    private int slidingWindowSize = 10;

    @Min(1)
    private int minimumNumberOfCalls = 5;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private float failureRateThreshold = 50.0f;

    @NotNull
    private Duration waitDurationInOpenState = Duration.ofSeconds(10);

    @Min(1)
    private int permittedNumberOfCallsInHalfOpenState = 3;
}
