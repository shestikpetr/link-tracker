package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
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
                    List<ChatLink> updated = entry.getValue().stream()
                            .filter(cl -> lastActivity.isAfter(cl.link().lastCheckedAt()))
                            .toList();

                    if (!updated.isEmpty()) {
                        List<Long> chatIds =
                                updated.stream().map(ChatLink::chatId).toList();
                        linkNotifier.notify(url, chatIds);
                        linkRepository.updateLastChecked(
                                updated.getFirst().link().id(), lastActivity);
                    }
                });
            } catch (Exception e) {
                log.atError()
                        .setMessage("Ошибка при проверке ссылки")
                        .addKeyValue("url", url)
                        .setCause(e)
                        .log();
            }
        }
    }
}
