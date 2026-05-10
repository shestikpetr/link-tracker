package backend.academy.linktracker.scrapper.configuration;

import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", matchIfMissing = true)
public class KafkaProducerConfiguration {

    @Bean
    public NewTopic linkUpdatesTopic(KafkaProperties kafkaProperties) {
        return TopicBuilder.name(kafkaProperties.getTopicName())
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}
