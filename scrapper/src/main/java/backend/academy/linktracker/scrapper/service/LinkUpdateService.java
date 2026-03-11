package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkUpdateService {
    private final LinkRepository linkRepository;
    private final LinkChecker linkChecker;
    private final LinkNotifier linkNotifier;

    public void checkAndNotify() {
        for (var entry : linkRepository.findAllGroupedByChat().entrySet()) {
            Long chatId = entry.getKey();

            for (TrackedLink link : entry.getValue()) {
                try {
                    linkChecker.checkLink(link).ifPresent(lastActivity -> {
                        linkNotifier.notify(link, chatId);
                        linkRepository.updateLastChecked(link.id(), lastActivity);
                    });
                } catch (Exception e) {
                    log.error("Ошибка при проверке ссылки {} для чата {}: {}", link.url(), chatId, e.getMessage());
                }
            }
        }
    }
}
