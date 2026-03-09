package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkNotifier {
    private final BotClient botClient;

    public void notify(TrackedLink link, long chatId) {
        botClient.sendUpdate(new LinkUpdate(link.id(), link.url(), "Обновление", List.of(chatId)));
    }
}
