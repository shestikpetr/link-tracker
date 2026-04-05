package backend.academy.linktracker.scrapper.service;

import static backend.academy.linktracker.scrapper.service.TestFactory.trackedLink;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.utils.ListPartitioner;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkBatchProcessorTest {

    static final URI URL_A = URI.create("https://github.com/foo/a");
    static final URI URL_B = URI.create("https://github.com/foo/b");

    @Mock
    LinkRepository linkRepository;

    @Mock
    LinkUpdateService linkUpdateService;

    LinkBatchProcessor linkBatchProcessor;

    @BeforeEach
    void setUp() {
        SchedulerProperties props = new SchedulerProperties();
        props.setBatchSize(100);
        props.setThreadCount(2);
        linkBatchProcessor = new LinkBatchProcessor(linkRepository, linkUpdateService, props, new ListPartitioner());
    }

    @Test
    void processBatch_delegates_each_link_to_service() {
        var linkA = trackedLink(1L, URL_A, 120);
        var linkB = trackedLink(2L, URL_B, 120);

        Map<URI, List<ChatLink>> batch = new LinkedHashMap<>();
        batch.put(URL_A, List.of(new ChatLink(1L, linkA)));
        batch.put(URL_B, List.of(new ChatLink(2L, linkB)));
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt())).thenReturn(batch);

        linkBatchProcessor.processBatch();

        verify(linkUpdateService).processLink(eq(URL_A), any());
        verify(linkUpdateService).processLink(eq(URL_B), any());
    }

    @Test
    void processBatch_does_nothing_when_batch_is_empty() {
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt())).thenReturn(Map.of());

        linkBatchProcessor.processBatch();

        verify(linkUpdateService, never()).processLink(any(), any());
    }

    @Test
    void processBatch_error_in_one_link_does_not_affect_others() {
        var linkA = trackedLink(1L, URL_A, 120);
        var linkB = trackedLink(2L, URL_B, 120);

        Map<URI, List<ChatLink>> batch = new LinkedHashMap<>();
        batch.put(URL_A, List.of(new ChatLink(1L, linkA)));
        batch.put(URL_B, List.of(new ChatLink(2L, linkB)));
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt())).thenReturn(batch);

        doThrow(new RuntimeException("API error")).when(linkUpdateService).processLink(eq(URL_A), any());

        linkBatchProcessor.processBatch();

        verify(linkUpdateService).processLink(eq(URL_B), any());
    }
}
