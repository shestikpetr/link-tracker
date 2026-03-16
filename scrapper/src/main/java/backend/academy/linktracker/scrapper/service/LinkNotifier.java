package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkNotifier {
    private final BotClient botClient;

    public void notify(URI url, List<Long> chatIds) {
        log.atInfo()
                .setMessage("Отправка обновления")
                .addKeyValue("url", url)
                .addKeyValue("chatIds", chatIds)
                .log();
        botClient.sendUpdate(new LinkUpdate(null, url, "Обновление", chatIds));
    }
}
