package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
                    List<Long> chatIds = new ArrayList<>();
                    for (ChatLink chatLink : entry.getValue()) {
                        if (lastActivity.isAfter(chatLink.link().lastCheckedAt())) {
                            chatIds.add(chatLink.chatId());
                            linkRepository.updateLastChecked(chatLink.link().id(), lastActivity);
                        }
                    }
                    if (!chatIds.isEmpty()) {
                        linkNotifier.notify(url, chatIds);
                    }
                });
            } catch (Exception e) {
                log.atError()
                        .setMessage("Ошибка при проверке ссылки")
                        .addKeyValue("url", url)
                        .addKeyValue("error", e.getMessage())
                        .log();
            }
        }
    }
}
