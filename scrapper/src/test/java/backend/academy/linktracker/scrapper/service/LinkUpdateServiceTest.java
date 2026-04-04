package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.properties.SchedulerProperties;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateServiceTest {

    static final URI URL_A = URI.create("https://github.com/foo/a");
    static final URI URL_B = URI.create("https://github.com/foo/b");

    @Mock
    LinkRepository linkRepository;

    @Mock
    LinkChecker linkChecker;

    @Mock
    NotificationSender notificationSender;

    LinkUpdateService linkUpdateService;

    @BeforeEach
    void setUp() {
        SchedulerProperties props = new SchedulerProperties();
        props.setBatchSize(100);
        props.setThreadCount(2);
        linkUpdateService = new LinkUpdateService(linkRepository, linkChecker, notificationSender, props);
    }

    @Test
    void checkAndNotify_notifies_only_chat_tracking_updated_link() {
        var linkA =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));
        var linkB =
                new TrackedLink(2L, URL_B, List.of(), List.of(), Instant.now().minusSeconds(120));

        Map<URI, List<ChatLink>> batch = new LinkedHashMap<>();
        batch.put(URL_A, List.of(new ChatLink(1L, linkA)));
        batch.put(URL_B, List.of(new ChatLink(2L, linkB)));
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt())).thenReturn(batch);

        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any()))
                .thenReturn(List.of(new LinkUpdateInfo(newActivity, "update A")));
        when(linkChecker.checkUpdates(eq(URL_B), any())).thenReturn(List.of());

        linkUpdateService.checkAndNotify();

        verify(notificationSender).send(eq(URL_A), anyString(), eq(List.of(1L)));
        verify(notificationSender, never()).send(eq(URL_B), anyString(), anyList());
    }

    @Test
    void checkAndNotify_does_not_notify_when_no_updates() {
        var link = new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now());
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt()))
                .thenReturn(Map.of(URL_A, List.of(new ChatLink(1L, link))));
        when(linkChecker.checkUpdates(eq(URL_A), any())).thenReturn(List.of());

        linkUpdateService.checkAndNotify();

        verify(notificationSender, never()).send(any(URI.class), anyString(), anyList());
    }

    @Test
    void checkAndNotify_updates_lastCheckedAt_after_notification() {
        var link =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(60));
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt()))
                .thenReturn(Map.of(URL_A, List.of(new ChatLink(1L, link))));

        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any())).thenReturn(List.of(new LinkUpdateInfo(newActivity, "update")));

        linkUpdateService.checkAndNotify();

        verify(linkRepository).updateLastChecked(1L, newActivity);
    }

    @Test
    void checkAndNotify_notifies_both_chats_tracking_the_same_url() {
        var linkChat1 =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));
        var linkChat2 =
                new TrackedLink(2L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));

        when(linkRepository.findStaleLinksGroupedByUrl(anyInt()))
                .thenReturn(Map.of(URL_A, List.of(new ChatLink(10L, linkChat1), new ChatLink(20L, linkChat2))));

        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any())).thenReturn(List.of(new LinkUpdateInfo(newActivity, "update")));

        linkUpdateService.checkAndNotify();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> chatIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(notificationSender).send(eq(URL_A), anyString(), chatIdsCaptor.capture());
        assertThat(chatIdsCaptor.getValue()).containsExactlyInAnyOrder(10L, 20L);

        verify(linkChecker, times(1)).checkUpdates(eq(URL_A), any());
    }

    @Test
    void checkAndNotify_error_in_one_link_does_not_affect_others() {
        var linkA =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));
        var linkB =
                new TrackedLink(2L, URL_B, List.of(), List.of(), Instant.now().minusSeconds(120));

        Map<URI, List<ChatLink>> batch = new LinkedHashMap<>();
        batch.put(URL_A, List.of(new ChatLink(1L, linkA)));
        batch.put(URL_B, List.of(new ChatLink(2L, linkB)));
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt())).thenReturn(batch);

        when(linkChecker.checkUpdates(eq(URL_A), any())).thenThrow(new RuntimeException("API error"));
        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_B), any()))
                .thenReturn(List.of(new LinkUpdateInfo(newActivity, "update B")));

        linkUpdateService.checkAndNotify();

        verify(notificationSender).send(eq(URL_B), anyString(), eq(List.of(2L)));
    }

    @Test
    void checkAndNotify_sends_description_from_handler() {
        var link =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(60));
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt()))
                .thenReturn(Map.of(URL_A, List.of(new ChatLink(1L, link))));

        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any()))
                .thenReturn(List.of(new LinkUpdateInfo(newActivity, "Новый Issue: Bug report")));

        linkUpdateService.checkAndNotify();

        verify(notificationSender).send(eq(URL_A), eq("Новый Issue: Bug report"), eq(List.of(1L)));
    }

    @Test
    void checkAndNotify_does_nothing_when_batch_is_empty() {
        when(linkRepository.findStaleLinksGroupedByUrl(anyInt())).thenReturn(Map.of());

        linkUpdateService.checkAndNotify();

        verify(notificationSender, never()).send(any(), anyString(), anyList());
        verify(linkChecker, never()).checkUpdates(any(), any());
    }
}
