package backend.academy.linktracker.scrapper.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        when(linkRepository.findAllGroupedByChat()).thenReturn(Map.of(1L, List.of(linkA), 2L, List.of(linkB)));

        Instant newActivity = Instant.now();
        when(linkChecker.checkLink(linkA)).thenReturn(Optional.of(newActivity));
        when(linkChecker.checkLink(linkB)).thenReturn(Optional.empty());

        linkUpdateService.checkAndNotify();

        verify(linkNotifier).notify(linkA, 1L);
        verify(linkNotifier, never()).notify(eq(linkB), anyLong());
    }

    @Test
    void checkAndNotify_does_not_notify_when_no_updates() {
        var link = new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now());
        when(linkRepository.findAllGroupedByChat()).thenReturn(Map.of(1L, List.of(link)));
        when(linkChecker.checkLink(link)).thenReturn(Optional.empty());

        linkUpdateService.checkAndNotify();

        verify(linkNotifier, never()).notify(any(), anyLong());
    }

    @Test
    void checkAndNotify_updates_lastCheckedAt_after_notification() {
        var link =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(60));
        when(linkRepository.findAllGroupedByChat()).thenReturn(Map.of(1L, List.of(link)));

        Instant newActivity = Instant.now();
        when(linkChecker.checkLink(link)).thenReturn(Optional.of(newActivity));

        linkUpdateService.checkAndNotify();

        verify(linkRepository).updateLastChecked(1L, newActivity);
    }

    @Test
    void checkAndNotify_notifies_both_chats_tracking_the_same_url() {
        var linkChat1 =
                new TrackedLink(1L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));
        var linkChat2 =
                new TrackedLink(2L, URL_A, List.of(), List.of(), Instant.now().minusSeconds(120));

        when(linkRepository.findAllGroupedByChat())
                .thenReturn(Map.of(10L, List.of(linkChat1), 20L, List.of(linkChat2)));

        Instant newActivity = Instant.now();
        when(linkChecker.checkLink(linkChat1)).thenReturn(Optional.of(newActivity));
        when(linkChecker.checkLink(linkChat2)).thenReturn(Optional.of(newActivity));

        linkUpdateService.checkAndNotify();

        verify(linkNotifier).notify(linkChat1, 10L);
        verify(linkNotifier).notify(linkChat2, 20L);
    }
}
