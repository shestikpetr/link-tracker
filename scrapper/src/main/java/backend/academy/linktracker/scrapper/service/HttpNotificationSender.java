package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.notification.transport", havingValue = "HTTP")
public class HttpNotificationSender implements NotificationSender {
    private final BotClient botClient;

    @Override
    public void send(URI url, String description, List<Long> chatIds) {
        log.atInfo()
                .setMessage("Отправка обновления")
                .addKeyValue("url", url)
                .addKeyValue("chatIds", chatIds)
                .log();
        botClient.sendUpdate(new LinkUpdate(url, description, chatIds));
    }
}
