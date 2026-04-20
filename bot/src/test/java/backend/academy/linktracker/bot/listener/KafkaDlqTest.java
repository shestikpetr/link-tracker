package backend.academy.linktracker.bot.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.KafkaTestConfiguration;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(
        properties = {
            "app.notification.transport=KAFKA",
            "app.kafka.topic-name=link-updates-dlq-src-test",
            "app.kafka.dlq-topic-name=link-updates-dlq-sink-test",
            "app.kafka.max-retries=2",
            "spring.autoconfigure.exclude=",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false",
            "spring.main.allow-bean-definition-overriding=true"
        })
@ActiveProfiles("test")
@EnableWireMock
@Import({KafkaTestConfiguration.class, KafkaDlqTest.SingleBrokerTopicConfig.class})
class KafkaDlqTest {

    @TestConfiguration
    static class SingleBrokerTopicConfig {
        @Bean
        NewTopic linkUpdatesDlqTopic(@Value("${app.kafka.dlq-topic-name}") String dlqTopicName) {
            return TopicBuilder.name(dlqTopicName).partitions(1).replicas(1).build();
        }

        @Bean
        NewTopic linkUpdatesTopic(@Value("${app.kafka.topic-name}") String topicName) {
            return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
        }
    }

    @MockitoBean
    LinkUpdateNotifier linkUpdateNotifier;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    KafkaConnectionDetails kafkaConnectionDetails;

    @Value("${app.kafka.topic-name}")
    String topicName;

    @Value("${app.kafka.dlq-topic-name}")
    String dlqTopicName;

    @Test
    void message_isSentToDlq_whenNotifierKeepsThrowing() {
        doThrow(new RuntimeException("boom")).when(linkUpdateNotifier).notify(any(LinkUpdate.class));

        LinkUpdate update = new LinkUpdate(1L, URI.create("https://github.com/foo/bar"), "desc", List.of(42L));
        kafkaTemplate.send(topicName, update.url().toString(), update);

        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(linkUpdateNotifier, atLeast(3)).notify(any(LinkUpdate.class)));

        ConsumerRecord<String, LinkUpdate> dlqRecord = readOneFromDlq();
        assertThat(dlqRecord.key()).isEqualTo(update.url().toString());
        assertThat(dlqRecord.value().url()).isEqualTo(update.url());
    }

    private ConsumerRecord<String, LinkUpdate> readOneFromDlq() {
        JacksonJsonDeserializer<LinkUpdate> valueDeserializer = new JacksonJsonDeserializer<>(LinkUpdate.class);
        valueDeserializer.addTrustedPackages("*");
        valueDeserializer.setUseTypeHeaders(false);

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlq-test-consumer-" + System.nanoTime(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        DefaultKafkaConsumerFactory<String, LinkUpdate> factory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), valueDeserializer);
        try (Consumer<String, LinkUpdate> consumer = factory.createConsumer()) {
            consumer.subscribe(List.of(dlqTopicName));
            return await().atMost(30, TimeUnit.SECONDS).until(() -> {
                ConsumerRecords<String, LinkUpdate> records = consumer.poll(Duration.ofMillis(500));
                return records.iterator().hasNext() ? records.iterator().next() : null;
            }, r -> r != null);
        }
    }
}
