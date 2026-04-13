package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.exceptions.LinkNotFoundException;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.state.ChatStateService;
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
class UntrackStateHandlerTest {

    static final long CHAT_ID = 123L;
    static final String URL = "https://github.com/foo/bar";

    @Mock
    LinkTrackingService linkTrackingService;

    @Mock
    ChatStateService chatStateService;

    UntrackStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UntrackStateHandler(linkTrackingService, chatStateService);
    }

    @Test
    void removes_link_successfully_and_clears_state() {
        SendMessage response = handler.handleInput(buildUpdate());

        assertThat(text(response)).isEqualTo("Ссылка удалена.");
        verify(linkTrackingService).removeLink(CHAT_ID, URI.create(URL));
        verify(chatStateService).clearSession(CHAT_ID);
    }

    @Test
    void link_not_found_shows_error_and_preserves_state() {
        doThrow(new LinkNotFoundException("Ссылка не найдена"))
                .when(linkTrackingService)
                .removeLink(anyLong(), any());

        SendMessage response = handler.handleInput(buildUpdate());

        assertThat(text(response)).isEqualTo("Ссылка не найдена");
        verify(chatStateService, never()).clearSession(CHAT_ID);
    }

    private String text(SendMessage message) {
        return (String) message.getParameters().get("text");
    }

    private Update buildUpdate() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(CHAT_ID);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(URL);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }
}
