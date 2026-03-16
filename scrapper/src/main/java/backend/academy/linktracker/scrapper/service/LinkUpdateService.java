package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
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
        for (var entry : linkRepository.findAllGroupedByUrl().entrySet()) {
            URI url = entry.getKey();
            try {
                linkChecker.getLastActivity(url).ifPresent(lastActivity -> {
                    for (var chatLink : entry.getValue()) {
                        if (lastActivity.isAfter(chatLink.link().lastCheckedAt())) {
                            linkNotifier.notify(chatLink.link(), chatLink.chatId());
                            linkRepository.updateLastChecked(chatLink.link().id(), lastActivity);
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Ошибка при проверке ссылки {}: {}", url, e.getMessage());
            }
        }
    }
}
