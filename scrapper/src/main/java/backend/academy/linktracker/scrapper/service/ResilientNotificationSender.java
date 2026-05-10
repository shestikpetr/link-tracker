package backend.academy.linktracker.scrapper.service;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "app.kafka.enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class ResilientNotificationSender implements NotificationSender {

    private final HttpNotificationSender httpSender;
    private final KafkaNotificationSender kafkaSender;

    @Override
    public void send(URI url, String description, List<Long> chatIds) {
        try {
            httpSender.send(url, description, chatIds);
        } catch (Exception e) {
            log.atWarn()
                    .setMessage("Основной транспорт HTTP недоступен, переключение на Kafka")
                    .addKeyValue("url", url)
                    .addKeyValue("error", e.getClass().getSimpleName())
                    .addKeyValue("message", e.getMessage())
                    .log();
            kafkaSender.send(url, description, chatIds);
        }
    }
}
