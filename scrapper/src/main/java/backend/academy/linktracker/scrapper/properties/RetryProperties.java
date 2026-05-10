package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Set;
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
@ConfigurationProperties(prefix = "app.resilience.retry")
public class RetryProperties {

    @Min(1)
    private int maxAttempts = 3;

    @NotNull
    private Duration waitDuration = Duration.ofMillis(500);

    @NotEmpty
    private Set<Integer> retryableStatuses = Set.of(500, 502, 503, 504);
}
