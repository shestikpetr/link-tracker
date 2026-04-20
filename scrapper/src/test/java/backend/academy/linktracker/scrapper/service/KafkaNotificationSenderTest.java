package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import backend.academy.linktracker.scrapper.KafkaTestConfiguration;
import backend.academy.linktracker.scrapper.TestcontainersConfiguration;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "app.notification.transport=KAFKA",
            "app.kafka.topic-name=link-updates-test",
            "spring.autoconfigure.exclude=",
            "spring.kafka.producer.acks=1",
            "spring.kafka.producer.properties.enable.idempotence=false",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false",
            "spring.main.allow-bean-definition-overriding=true"
        })
@ActiveProfiles("test")
@Import({
    TestcontainersConfiguration.class,
    KafkaTestConfiguration.class,
    KafkaNotificationSenderTest.SingleBrokerTopicConfig.class
})
class KafkaNotificationSenderTest {

    @TestConfiguration
    static class SingleBrokerTopicConfig {
        @Bean
        NewTopic linkUpdatesTopic(@Value("${app.kafka.topic-name}") String topicName) {
            return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
        }
    }

    @Autowired
    NotificationSender notificationSender;

    @Autowired
    KafkaConnectionDetails kafkaConnectionDetails;

    @Value("${app.kafka.topic-name}")
    String topicName;

    @Test
    void send_publishesLinkUpdateToTopic() {
        URI url = URI.create("https://github.com/foo/bar");
        String description = "new commit";
        List<Long> chatIds = List.of(1L, 2L);

        notificationSender.send(url, description, chatIds);

        ConsumerRecord<String, LinkUpdate> record = readOne();
        assertThat(record.key()).isEqualTo(url.toString());
        assertThat(record.value().url()).isEqualTo(url);
        assertThat(record.value().description()).isEqualTo(description);
        assertThat(record.value().tgChatIds()).containsExactlyInAnyOrderElementsOf(chatIds);
    }

    private ConsumerRecord<String, LinkUpdate> readOne() {
        JacksonJsonDeserializer<LinkUpdate> valueDeserializer = new JacksonJsonDeserializer<>(LinkUpdate.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeHeaders(false);

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaConnectionDetails.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "test-consumer-" + System.nanoTime(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false);

        DefaultKafkaConsumerFactory<String, LinkUpdate> factory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
        try (Consumer<String, LinkUpdate> consumer = factory.createConsumer()) {
            consumer.subscribe(List.of(topicName));
            return await().atMost(30, TimeUnit.SECONDS)
                    .until(
                            () -> {
                                ConsumerRecords<String, LinkUpdate> records = consumer.poll(Duration.ofMillis(500));
                                return records.iterator().hasNext()
                                        ? records.iterator().next()
                                        : null;
                            },
                            r -> r != null);
        }
    }
}
