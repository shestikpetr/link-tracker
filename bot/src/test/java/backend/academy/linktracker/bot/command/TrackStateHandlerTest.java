package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_TAGS;
import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.ScrapperClient;
import backend.academy.linktracker.bot.dto.AddLinkRequest;
import backend.academy.linktracker.bot.exceptions.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.bot.state.ChatStateService;
import backend.academy.linktracker.bot.utils.TagParser;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackStateHandlerTest {

    static final long CHAT_ID = 123L;

    @Mock
    ScrapperClient scrapperClient;

    @Mock
    ChatStateService chatStateService;

    TrackStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TrackStateHandler(scrapperClient, chatStateService, new TagParser());
    }

    @Test
    void waiting_url_valid_url_transitions_to_waiting_tags() {
        when(chatStateService.getState(CHAT_ID)).thenReturn(Optional.of(WAITING_TRACK_URL));

        SendMessage response = handler.handleInput(buildUpdate("https://github.com/foo/bar"));

        assertThat(text(response)).isEqualTo("Введите теги:");
        verify(chatStateService).setPendingUrl(CHAT_ID, URI.create("https://github.com/foo/bar"));
        verify(chatStateService).setState(CHAT_ID, WAITING_TRACK_TAGS);
    }

    @Test
    void waiting_url_invalid_url_returns_error_without_state_change() {
        when(chatStateService.getState(CHAT_ID)).thenReturn(Optional.of(WAITING_TRACK_URL));

        SendMessage response = handler.handleInput(buildUpdate("не ссылка"));

        assertThat(text(response)).contains("Некорректная");
        verify(chatStateService, never()).setPendingUrl(anyLong(), any());
        verify(chatStateService, never()).setState(anyLong(), any());
    }

    @Test
    void waiting_tags_adds_link_and_clears_state() {
        URI url = URI.create("https://github.com/foo/bar");
        when(chatStateService.getState(CHAT_ID)).thenReturn(Optional.of(WAITING_TRACK_TAGS));
        when(chatStateService.getPendingUrl(CHAT_ID)).thenReturn(url);

        SendMessage response = handler.handleInput(buildUpdate("тег1, тег2"));

        assertThat(text(response)).isEqualTo("Ссылка добавлена.");
        verify(scrapperClient).addLink(CHAT_ID, new AddLinkRequest(url, List.of("тег1", "тег2"), List.of()));
        verify(chatStateService).clearState(CHAT_ID);
    }

    @Test
    void waiting_tags_already_tracked_shows_error_and_clears_state() {
        URI url = URI.create("https://github.com/foo/bar");
        when(chatStateService.getState(CHAT_ID)).thenReturn(Optional.of(WAITING_TRACK_TAGS));
        when(chatStateService.getPendingUrl(CHAT_ID)).thenReturn(url);
        doThrow(new LinkAlreadyTrackedException()).when(scrapperClient).addLink(anyLong(), any());

        SendMessage response = handler.handleInput(buildUpdate("тег1"));

        assertThat(text(response)).isEqualTo("Ссылка уже отслеживается.");
        verify(chatStateService).clearState(CHAT_ID);
    }

    @Test
    void waiting_tags_unsupported_link_shows_error_and_clears_state() {
        URI url = URI.create("https://github.com/foo/bar");
        when(chatStateService.getState(CHAT_ID)).thenReturn(Optional.of(WAITING_TRACK_TAGS));
        when(chatStateService.getPendingUrl(CHAT_ID)).thenReturn(url);
        doThrow(new UnsupportedLinkException()).when(scrapperClient).addLink(anyLong(), any());

        SendMessage response = handler.handleInput(buildUpdate("тег1"));

        assertThat(text(response)).contains("не поддерживается");
        verify(chatStateService).clearState(CHAT_ID);
    }

    private String text(SendMessage message) {
        return (String) message.getParameters().get("text");
    }

    private Update buildUpdate(String text) {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(CHAT_ID);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(text);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }
}
