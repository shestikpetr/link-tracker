package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.properties.KafkaProperties;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", matchIfMissing = true)
public class KafkaConsumerConfiguration {

    @Bean
    public NewTopic linkUpdatesDlqTopic(KafkaProperties kafkaProperties) {
        return TopicBuilder.name(kafkaProperties.getDlqTopicName())
                .partitions(1)
                .replicas(3)
                .build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<Object, Object> kafkaTemplate, KafkaProperties kafkaProperties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate, (_, _) -> new TopicPartition(kafkaProperties.getDlqTopicName(), -1));
        FixedBackOff backOff = new FixedBackOff(1000L, kafkaProperties.getMaxRetries());
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        handler.addNotRetryableExceptions(
                DeserializationException.class, MessageConversionException.class, IllegalArgumentException.class);
        return handler;
    }
}
