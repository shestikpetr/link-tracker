package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.dto.LinkUpdate;
import backend.academy.linktracker.bot.service.LinkUpdateNotifier;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.kafka.enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaLinkUpdateListener {
    private final LinkUpdateNotifier linkUpdateNotifier;
    private final Validator validator;

    @KafkaListener(topics = "${app.kafka.topic-name}", groupId = "${app.kafka.consumer-group-id}")
    public void listen(LinkUpdate update) {
        Set<ConstraintViolation<LinkUpdate>> violations = validator.validate(update);
        if (!violations.isEmpty()) {
            log.atWarn()
                    .setMessage("Невалидное сообщение из Kafka")
                    .addKeyValue("violations", violations)
                    .log();
            throw new IllegalArgumentException("Невалидное сообщение: " + violations);
        }
        log.atInfo()
                .setMessage("Получено обновление из Kafka")
                .addKeyValue("url", update.url())
                .log();
        linkUpdateNotifier.notify(update);
    }
}
