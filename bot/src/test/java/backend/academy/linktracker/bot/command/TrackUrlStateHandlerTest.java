package backend.academy.linktracker.bot.command;

import static backend.academy.linktracker.bot.state.ChatState.WAITING_TRACK_TAGS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.state.ChatStateService;
import backend.academy.linktracker.bot.utils.UrlValidator;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackUrlStateHandlerTest {

    static final long CHAT_ID = 123L;

    @Mock
    ChatStateService chatStateService;

    TrackUrlStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TrackUrlStateHandler(chatStateService, new UrlValidator());
    }

    @Test
    void valid_url_stores_pending_url_and_transitions_to_waiting_tags() {
        SendMessage response = handler.handleInput(buildUpdate("https://github.com/foo/bar"));

        assertThat(text(response)).isEqualTo("Введите теги:");
        verify(chatStateService).setPendingUrl(CHAT_ID, URI.create("https://github.com/foo/bar"));
        verify(chatStateService).setState(CHAT_ID, WAITING_TRACK_TAGS);
    }

    @Test
    void invalid_url_returns_error_without_state_change() {
        SendMessage response = handler.handleInput(buildUpdate("не ссылка"));

        assertThat(text(response)).contains("Некорректная");
        verify(chatStateService, never()).setPendingUrl(anyLong(), any());
        verify(chatStateService, never()).setState(anyLong(), any());
    }

    @Test
    void relative_uri_returns_error_without_state_change() {
        SendMessage response = handler.handleInput(buildUpdate("вов"));

        assertThat(text(response)).contains("Некорректная");
        verify(chatStateService, never()).setPendingUrl(anyLong(), any());
        verify(chatStateService, never()).setState(anyLong(), any());
    }

    @Test
    void non_http_scheme_returns_error_without_state_change() {
        SendMessage response = handler.handleInput(buildUpdate("ftp://example.com/file"));

        assertThat(text(response)).contains("Некорректная");
        verify(chatStateService, never()).setPendingUrl(anyLong(), any());
        verify(chatStateService, never()).setState(anyLong(), any());
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
