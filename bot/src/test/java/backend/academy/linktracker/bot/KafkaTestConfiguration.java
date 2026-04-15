package backend.academy.linktracker.bot;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class KafkaTestConfiguration {

    private static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.1")).withReuse(true);

    static {
        KAFKA.start();
    }

    @Bean
    @ServiceConnection
    KafkaContainer kafkaContainer() {
        return KAFKA;
    }
}
