package backend.academy.linktracker.bot.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.KafkaTestConfiguration;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(
        properties = {
            "app.notification.transport=KAFKA",
            "app.kafka.topic-name=link-updates-dlq-src-test",
            "app.kafka.dlq-topic-name=link-updates-dlq-sink-test",
            "app.kafka.max-retries=2",
            "spring.autoconfigure.exclude[0]=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "spring.autoconfigure.exclude[1]=org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "spring.autoconfigure.exclude[2]=org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "spring.autoconfigure.exclude[3]=org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "spring.autoconfigure.exclude[4]=org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration",
            "spring.autoconfigure.exclude[5]=org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration",
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

    @BeforeEach
    void resetMocks() {
        reset(linkUpdateNotifier);
    }

    @Test
    void businessError_retriesThenSendsToDlq() {
        doThrow(new RuntimeException("boom")).when(linkUpdateNotifier).notify(any(LinkUpdate.class));

        String key = "https://github.com/foo/bar-retry";
        LinkUpdate update = new LinkUpdate(1L, URI.create(key), "desc", List.of(42L));
        kafkaTemplate.send(topicName, key, update);

        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(linkUpdateNotifier, atLeast(3)).notify(any(LinkUpdate.class)));

        ConsumerRecord<String, byte[]> dlqRecord = readFromDlq(key, Duration.ofSeconds(30));
        assertThat(dlqRecord.key()).isEqualTo(key);
    }

    @Test
    void invalidJson_isSentToDlqWithoutRetries() throws Exception {
        String key = "https://github.com/foo/bar-invalid-json";
        sendInvalidJson(topicName, key);

        ConsumerRecord<String, byte[]> dlqRecord = readFromDlq(key, Duration.ofSeconds(15));
        assertThat(dlqRecord.key()).isEqualTo(key);
        verify(linkUpdateNotifier, never()).notify(any(LinkUpdate.class));
    }

    @Test
    void validationFailure_isSentToDlqWithoutRetries() {
        String key = "null-url-test";
        LinkUpdate invalidUpdate = new LinkUpdate(1L, null, "desc", List.of(42L));

        kafkaTemplate.send(topicName, key, invalidUpdate);

        ConsumerRecord<String, byte[]> dlqRecord = readFromDlq(key, Duration.ofSeconds(15));
        assertThat(dlqRecord.key()).isEqualTo(key);
        verify(linkUpdateNotifier, atMost(0)).notify(any(LinkUpdate.class));
    }

    private void sendInvalidJson(String topic, String key) throws Exception {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            producer.send(new ProducerRecord<>(topic, key, "not a json")).get();
        }
    }

    private ConsumerRecord<String, byte[]> readFromDlq(String expectedKey, Duration timeout) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                kafkaConnectionDetails.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG,
                "dlq-test-consumer-" + System.nanoTime(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest",
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                false);

        DefaultKafkaConsumerFactory<String, byte[]> factory =
                new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new ByteArrayDeserializer());
        try (Consumer<String, byte[]> consumer = factory.createConsumer()) {
            consumer.subscribe(List.of(dlqTopicName));
            return await().atMost(timeout.toSeconds(), TimeUnit.SECONDS)
                    .until(
                            () -> {
                                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
                                for (ConsumerRecord<String, byte[]> r : records) {
                                    if (expectedKey.equals(r.key())) {
                                        return r;
                                    }
                                }
                                return null;
                            },
                            Objects::nonNull);
        }
    }
}
