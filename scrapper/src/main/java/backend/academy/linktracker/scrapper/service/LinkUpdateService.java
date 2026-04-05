package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
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
    private final NotificationSender notificationSender;

    public void processLink(URI url, List<ChatLink> chatLinks) {
        Instant oldestLastChecked = chatLinks.stream()
                .map(cl -> cl.link().lastCheckedAt())
                .min(Instant::compareTo)
                .orElse(Instant.now());

        List<LinkUpdateInfo> updates = linkChecker.checkUpdates(url, oldestLastChecked);
        if (updates.isEmpty()) {
            return;
        }

        Instant latestUpdate = updates.stream()
                .map(LinkUpdateInfo::timestamp)
                .max(Instant::compareTo)
                .orElse(Instant.now());

        List<Long> chatIds = new ArrayList<>();
        for (ChatLink chatLink : chatLinks) {
            if (latestUpdate.isAfter(chatLink.link().lastCheckedAt())) {
                chatIds.add(chatLink.chatId());
                linkRepository.updateLastChecked(chatLink.link().id(), latestUpdate);
            }
        }

        if (!chatIds.isEmpty()) {
            String description = updates.stream()
                    .map(LinkUpdateInfo::description)
                    .reduce((a, b) -> a + "\n\n" + b)
                    .orElse("");
            notificationSender.send(url, description, chatIds);
        }
    }
}
