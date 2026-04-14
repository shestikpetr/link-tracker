package backend.academy.linktracker.scrapper.properties;

import jakarta.validation.constraints.NotEmpty;
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
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaProperties {
    @NotEmpty
    private String topicName;
}
