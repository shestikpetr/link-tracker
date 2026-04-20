package backend.academy.linktracker.bot.listener;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.KafkaTestConfiguration;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(
        properties = {
            "app.notification.transport=KAFKA",
            "app.kafka.topic-name=link-updates-test",
            "app.kafka.dlq-topic-name=link-updates-dlq-test",
            "spring.autoconfigure.exclude=",
            "spring.kafka.producer.properties.spring.json.add.type.headers=false",
            "spring.main.allow-bean-definition-overriding=true"
        })
@ActiveProfiles("test")
@EnableWireMock
@Import({KafkaTestConfiguration.class, KafkaLinkUpdateListenerTest.SingleBrokerTopicConfig.class})
class KafkaLinkUpdateListenerTest {

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

    @Value("${app.kafka.topic-name}")
    String topicName;

    @Test
    void listen_delegatesValidMessageToNotifier() {
        LinkUpdate update = new LinkUpdate(1L, URI.create("https://github.com/foo/bar"), "new commit", List.of(42L));

        kafkaTemplate.send(topicName, update.url().toString(), update);

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> verify(linkUpdateNotifier).notify(any(LinkUpdate.class)));
    }
}
