package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.properties.KafkaProperties;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.notification.transport", havingValue = "KAFKA", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaNotificationSender implements NotificationSender {
    private final KafkaTemplate<String, LinkUpdate> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    @Override
    public void send(URI url, String description, List<Long> chatIds) {
        log.atInfo()
                .setMessage("Отправка обновления через Kafka")
                .addKeyValue("url", url)
                .addKeyValue("chatIds", chatIds)
                .log();
        LinkUpdate update = new LinkUpdate(url, description, chatIds);
        kafkaTemplate.send(kafkaProperties.getTopicName(), url.toString(), update);
    }
}
