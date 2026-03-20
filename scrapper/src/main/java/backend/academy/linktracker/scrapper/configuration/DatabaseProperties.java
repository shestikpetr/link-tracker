package backend.academy.linktracker.scrapper.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.database")
@Validated
public record DatabaseProperties(AccessType accessType) {
    public enum AccessType {
        INMEMORY,
        SQL,
        ORM,
    }
}
