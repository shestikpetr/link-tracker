package backend.academy.linktracker.scrapper.scheduler;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.dto.LinkUpdate;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.LinkChecker;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LinkUpdateScheduler {
    private final LinkRepository linkRepository;
    private final LinkChecker linkChecker;
    private final BotClient botClient;

    public LinkUpdateScheduler(LinkRepository linkRepository, LinkChecker linkChecker, BotClient botClient) {
        this.linkRepository = linkRepository;
        this.linkChecker = linkChecker;
        this.botClient = botClient;
    }

    @Scheduled(fixedDelayString = "${app.scheduler.interval}")
    public void checkUpdates() {
        for (var entry : linkRepository.findAllGroupedByChat().entrySet()) {
            Long chatId = entry.getKey();

            for (TrackedLink link : entry.getValue()) {
                linkChecker
                        .checkLink(link)
                        .ifPresent(_ -> botClient.sendUpdate(
                                new LinkUpdate(link.id(), link.url(), "Обновление", List.of(chatId))));
            }
        }
    }
}
