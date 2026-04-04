package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LinkUpdateService {
    private final LinkRepository linkRepository;
    private final LinkChecker linkChecker;
    private final NotificationSender notificationSender;
    private final SchedulerProperties schedulerProperties;
    private final ExecutorService executorService;

    public LinkUpdateService(
            LinkRepository linkRepository,
            LinkChecker linkChecker,
            NotificationSender notificationSender,
            SchedulerProperties schedulerProperties) {
        this.linkRepository = linkRepository;
        this.linkChecker = linkChecker;
        this.notificationSender = notificationSender;
        this.schedulerProperties = schedulerProperties;
        this.executorService = Executors.newFixedThreadPool(schedulerProperties.getThreadCount());
    }

    public void checkAndNotify() {
        Map<URI, List<ChatLink>> batch = linkRepository.findStaleLinksGroupedByUrl(schedulerProperties.getBatchSize());

        if (batch.isEmpty()) {
            return;
        }

        List<Map.Entry<URI, List<ChatLink>>> entries = new ArrayList<>(batch.entrySet());
        int threadCount = Math.min(schedulerProperties.getThreadCount(), entries.size());
        int chunkSize = (entries.size() + threadCount - 1) / threadCount;

        List<Future<List<URI>>> futures = new ArrayList<>();
        for (int i = 0; i < entries.size(); i += chunkSize) {
            List<Map.Entry<URI, List<ChatLink>>> chunk = entries.subList(i, Math.min(i + chunkSize, entries.size()));
            futures.add(executorService.submit(() -> processChunk(chunk)));
        }

        List<URI> failedUrls = new ArrayList<>();
        for (Future<List<URI>> future : futures) {
            try {
                failedUrls.addAll(future.get());
            } catch (Exception e) {
                log.atError()
                        .setMessage("Ошибка при ожидании результата потока")
                        .setCause(e)
                        .log();
            }
        }

        if (!failedUrls.isEmpty()) {
            log.atWarn()
                    .setMessage("Не удалось обработать ссылки")
                    .addKeyValue("count", failedUrls.size())
                    .addKeyValue("urls", failedUrls)
                    .log();
        }
    }

    private List<URI> processChunk(List<Map.Entry<URI, List<ChatLink>>> chunk) {
        List<URI> failed = new ArrayList<>();
        for (var entry : chunk) {
            try {
                processLink(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                failed.add(entry.getKey());
                log.atError()
                        .setMessage("Ошибка при проверке ссылки")
                        .addKeyValue("url", entry.getKey())
                        .setCause(e)
                        .log();
            }
        }
        return failed;
    }

    private void processLink(URI url, List<ChatLink> chatLinks) {
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
