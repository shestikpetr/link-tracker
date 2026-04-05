package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.utils.ListPartitioner;
import java.net.URI;
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
public class LinkBatchProcessor {
    private final LinkRepository linkRepository;
    private final LinkUpdateService linkUpdateService;
    private final SchedulerProperties schedulerProperties;
    private final ListPartitioner listPartitioner;
    private final ExecutorService executorService;

    public LinkBatchProcessor(
            LinkRepository linkRepository,
            LinkUpdateService linkUpdateService,
            SchedulerProperties schedulerProperties,
            ListPartitioner listPartitioner) {
        this.linkRepository = linkRepository;
        this.linkUpdateService = linkUpdateService;
        this.schedulerProperties = schedulerProperties;
        this.listPartitioner = listPartitioner;
        this.executorService = Executors.newFixedThreadPool(schedulerProperties.getThreadCount());
    }

    public void processBatch() {
        Map<URI, List<ChatLink>> batch = linkRepository.findStaleLinksGroupedByUrl(schedulerProperties.getBatchSize());

        if (batch.isEmpty()) {
            return;
        }

        List<Map.Entry<URI, List<ChatLink>>> entries = new ArrayList<>(batch.entrySet());
        int threadCount = Math.min(schedulerProperties.getThreadCount(), entries.size());

        List<List<Map.Entry<URI, List<ChatLink>>>> chunks = listPartitioner.partition(entries, threadCount);

        List<Future<List<URI>>> futures = new ArrayList<>();
        for (var chunk : chunks) {
            futures.add(executorService.submit(() -> processChunk(chunk)));
        }

        List<URI> failedUrls = collectFailures(futures);

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
                linkUpdateService.processLink(entry.getKey(), entry.getValue());
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

    private List<URI> collectFailures(List<Future<List<URI>>> futures) {
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
        return failedUrls;
    }
}
