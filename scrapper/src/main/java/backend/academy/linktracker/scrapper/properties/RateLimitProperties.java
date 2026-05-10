package backend.academy.linktracker.scrapper.properties;

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
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    @Min(1)
    private int capacity = 20;

    @NotNull
    private Duration refillPeriod = Duration.ofMinutes(1);
}
