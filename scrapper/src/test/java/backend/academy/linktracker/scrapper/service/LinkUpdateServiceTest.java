package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.model.ChatLink;
import backend.academy.linktracker.scrapper.model.TrackedLink;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
    LinkNotifier linkNotifier;

    @InjectMocks
    LinkUpdateService linkUpdateService;

    @Test
    void checkAndNotify_notifies_only_chat_tracking_updated_link() {
        var linkA =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));
        var linkB =
                new TrackedLink(2L, URL_B, List.of(), List.of(), Instant.now().minusSeconds(120));

        when(linkRepository.findAllGroupedByUrl())
                .thenReturn(Map.of(
                        URL_A, List.of(new ChatLink(1L, linkA)),
                        URL_B, List.of(new ChatLink(2L, linkB))));

        Instant newActivity = Instant.now();
        when(linkChecker.getLastActivity(URL_A)).thenReturn(Optional.of(newActivity));
        when(linkChecker.getLastActivity(URL_B)).thenReturn(Optional.empty());

        linkUpdateService.checkAndNotify();

        verify(linkNotifier).notify(URL_A, List.of(1L));
        verify(linkNotifier, never()).notify(eq(URL_B), anyList());
    }

    @Test
    void checkAndNotify_does_not_notify_when_no_updates() {
        var link = new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now());
        when(linkRepository.findAllGroupedByUrl()).thenReturn(Map.of(URL_A, List.of(new ChatLink(1L, link))));
        when(linkChecker.getLastActivity(URL_A)).thenReturn(Optional.empty());

        linkUpdateService.checkAndNotify();

        verify(linkNotifier, never()).notify(any(URI.class), anyList());
    }

    @Test
    void checkAndNotify_updates_lastCheckedAt_after_notification() {
        var link =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(60));
        when(linkRepository.findAllGroupedByUrl()).thenReturn(Map.of(URL_A, List.of(new ChatLink(1L, link))));

        Instant newActivity = Instant.now();
        when(linkChecker.getLastActivity(URL_A)).thenReturn(Optional.of(newActivity));

        linkUpdateService.checkAndNotify();

        verify(linkRepository).updateLastChecked(1L, newActivity);
    }

    @Test
    void checkAndNotify_notifies_both_chats_tracking_the_same_url_with_single_api_call() {
        var linkChat1 =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));
        var linkChat2 =
                new TrackedLink(2L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));

        when(linkRepository.findAllGroupedByUrl())
                .thenReturn(Map.of(URL_A, List.of(new ChatLink(10L, linkChat1), new ChatLink(20L, linkChat2))));

        Instant newActivity = Instant.now();
        when(linkChecker.getLastActivity(URL_A)).thenReturn(Optional.of(newActivity));

        linkUpdateService.checkAndNotify();

        verify(linkNotifier).notify(URL_A, List.of(10L, 20L));
        verify(linkChecker, times(1)).getLastActivity(URL_A);
    }
}
