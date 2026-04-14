package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotNull;
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
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {
    @NotNull
    private Transport transport = Transport.KAFKA;

    public enum Transport {
        HTTP,
        KAFKA
    }
}
