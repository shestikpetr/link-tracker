package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
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
        if (chatLinks.isEmpty()) {
            throw new IllegalArgumentException("chatLinks не должен быть пустым");
        }

        Instant oldestLastChecked = chatLinks.stream()
                .map(cl -> cl.link().lastCheckedAt())
                .min(Instant::compareTo)
                .orElseThrow();

        List<LinkUpdateInfo> updates = linkChecker.checkUpdates(url, oldestLastChecked);
        if (updates.isEmpty()) {
            return;
        }

        Instant latestUpdate = updates.stream()
                .map(LinkUpdateInfo::timestamp)
                .max(Instant::compareTo)
                .orElseThrow();

        List<Long> chatIds = chatLinks.stream()
                .filter(cl -> latestUpdate.isAfter(cl.link().lastCheckedAt()))
                .map(ChatLink::chatId)
                .toList();

        if (chatIds.isEmpty()) {
            return;
        }

        linkRepository.updateLastChecked(chatLinks.getFirst().link().id(), latestUpdate);

        String description = updates.stream().map(LinkUpdateInfo::description).collect(Collectors.joining("\n\n"));
        notificationSender.send(url, description, chatIds);
    }
}
