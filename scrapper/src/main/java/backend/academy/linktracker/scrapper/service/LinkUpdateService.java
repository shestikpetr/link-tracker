package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
                linkChecker.checkLink(link).ifPresent(lastActivity -> {
                    linkNotifier.notify(link, chatId);
                    linkRepository.updateLastChecked(link.id(), lastActivity);
                });
            }
        }
    }
}
