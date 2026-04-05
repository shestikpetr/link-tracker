package backend.academy.linktracker.scrapper.service;

import static backend.academy.linktracker.scrapper.service.TestFactory.trackedLink;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LinkUpdateServiceTest {

    static final URI URL_A = URI.create("https://github.com/foo/a");

    @Mock
    LinkRepository linkRepository;

    @Mock
    LinkChecker linkChecker;

    @Mock
    NotificationSender notificationSender;

    @InjectMocks
    LinkUpdateService linkUpdateService;

    @Test
    void processLink_notifies_chats_when_updates_found() {
        var link = trackedLink(1L, URL_A, 120);
        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any()))
                .thenReturn(List.of(new LinkUpdateInfo(newActivity, "update A")));

        linkUpdateService.processLink(URL_A, List.of(new ChatLink(1L, link)));

        verify(notificationSender).send(eq(URL_A), eq("update A"), eq(List.of(1L)));
    }

    @Test
    void processLink_does_not_notify_when_no_updates() {
        var link = trackedLink(1L, URL_A, 0);
        when(linkChecker.checkUpdates(eq(URL_A), any())).thenReturn(List.of());

        linkUpdateService.processLink(URL_A, List.of(new ChatLink(1L, link)));

        verify(notificationSender, never()).send(any(), anyString(), any());
    }

    @Test
    void processLink_updates_lastCheckedAt() {
        var link = trackedLink(1L, URL_A, 60);
        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any())).thenReturn(List.of(new LinkUpdateInfo(newActivity, "update")));

        linkUpdateService.processLink(URL_A, List.of(new ChatLink(1L, link)));

        verify(linkRepository).updateLastChecked(1L, newActivity);
    }

    @Test
    void processLink_notifies_all_chats_tracking_same_url() {
        var link1 = trackedLink(1L, URL_A, 120);
        var link2 = trackedLink(2L, URL_A, 120);
        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any())).thenReturn(List.of(new LinkUpdateInfo(newActivity, "update")));

        linkUpdateService.processLink(URL_A, List.of(new ChatLink(10L, link1), new ChatLink(20L, link2)));

        verify(notificationSender).send(eq(URL_A), anyString(), eq(List.of(10L, 20L)));
    }

    @Test
    void processLink_sends_description_from_handler() {
        var link = trackedLink(1L, URL_A, 60);
        Instant newActivity = Instant.now();
        when(linkChecker.checkUpdates(eq(URL_A), any()))
                .thenReturn(List.of(new LinkUpdateInfo(newActivity, "Новый Issue: Bug report")));

        linkUpdateService.processLink(URL_A, List.of(new ChatLink(1L, link)));

        verify(notificationSender).send(eq(URL_A), eq("Новый Issue: Bug report"), eq(List.of(1L)));
    }
}
