package backend.academy.linktracker.bot.listener;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import backend.academy.linktracker.bot.KafkaTestConfiguration;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
import backend.academy.linktracker.scrapper.service.KafkaNotificationSender;
import backend.academy.linktracker.scrapper.service.NotificationSender;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(
        properties = {
            "app.notification.transport=KAFKA",
            "app.kafka.topic-name=link-updates-e2e",
            "app.kafka.dlq-topic-name=link-updates-e2e-dlq",
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
@Import({KafkaTestConfiguration.class, ScraperKafkaBotIntegrationTest.ScraperSenderConfig.class})
class ScraperKafkaBotIntegrationTest {

    @TestConfiguration
    static class ScraperSenderConfig {
        @Bean
        NewTopic linkUpdatesTopic(@Value("${app.kafka.topic-name}") String topicName) {
            return TopicBuilder.name(topicName).partitions(1).replicas(1).build();
        }

        @Bean
        NewTopic linkUpdatesDlqTopic(@Value("${app.kafka.dlq-topic-name}") String dlqTopicName) {
            return TopicBuilder.name(dlqTopicName).partitions(1).replicas(1).build();
        }

        @Bean
        backend.academy.linktracker.scrapper.properties.KafkaProperties scraperKafkaProperties(
                @Value("${app.kafka.topic-name}") String topicName) {
            var props = new backend.academy.linktracker.scrapper.properties.KafkaProperties();
            props.setTopicName(topicName);
            return props;
        }

        @Bean
        NotificationSender scraperSender(
                KafkaConnectionDetails kafkaConnectionDetails,
                backend.academy.linktracker.scrapper.properties.KafkaProperties scraperKafkaProperties) {
            Map<String, Object> props = new HashMap<>();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionDetails.getBootstrapServers());
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
            props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, false);
            ProducerFactory<String, backend.academy.linktracker.scrapper.dto.LinkUpdate> factory =
                    new DefaultKafkaProducerFactory<>(props);
            return new KafkaNotificationSender(new KafkaTemplate<>(factory), scraperKafkaProperties);
        }
    }

    @MockitoBean
    LinkUpdateNotifier linkUpdateNotifier;

    @Autowired
    NotificationSender scraperSender;

    @Test
    void scraperPublishesUpdate_botConsumesAndNotifies() {
        URI url = URI.create("https://github.com/e2e/integration");
        String description = "new release";
        List<Long> chatIds = List.of(101L, 202L);

        scraperSender.send(url, description, chatIds);

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> verify(linkUpdateNotifier)
                .notify(argThat((LinkUpdate u) -> u != null
                        && u.url().equals(url)
                        && description.equals(u.description())
                        && u.tgChatIds() != null
                        && u.tgChatIds().containsAll(chatIds))));
    }
}
