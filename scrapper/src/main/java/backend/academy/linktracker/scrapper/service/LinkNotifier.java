package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkNotifier {
    private final BotClient botClient;

    public void notify(TrackedLink link, long chatId) {
        log.info("Отправка обновления по ссылке {} в чат {}", link.url(), chatId);
        botClient.sendUpdate(new LinkUpdate(link.id(), link.url(), "Обновление", List.of(chatId)));
    }
}
