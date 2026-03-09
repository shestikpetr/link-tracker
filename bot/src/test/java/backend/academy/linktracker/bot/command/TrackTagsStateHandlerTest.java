package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.exceptions.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exceptions.UnsupportedLinkException;
import backend.academy.linktracker.bot.service.LinkTrackingService;
import backend.academy.linktracker.bot.state.ChatStateService;
import backend.academy.linktracker.bot.utils.TagParser;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackTagsStateHandlerTest {

    static final long CHAT_ID = 123L;

    @Mock
    LinkTrackingService linkTrackingService;

    @Mock
    ChatStateService chatStateService;

    TrackTagsStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TrackTagsStateHandler(linkTrackingService, chatStateService, new TagParser());
    }

    @Test
    void adds_link_with_parsed_tags_and_clears_state() {
        URI url = URI.create("https://github.com/foo/bar");
        when(chatStateService.getPendingUrl(CHAT_ID)).thenReturn(url);

        SendMessage response = handler.handleInput(buildUpdate("тег1, тег2"));

        assertThat(text(response)).isEqualTo("Ссылка добавлена.");
        verify(linkTrackingService).addLink(CHAT_ID, url, List.of("тег1", "тег2"));
        verify(chatStateService).clearState(CHAT_ID);
    }

    @Test
    void already_tracked_shows_error_and_clears_state() {
        URI url = URI.create("https://github.com/foo/bar");
        when(chatStateService.getPendingUrl(CHAT_ID)).thenReturn(url);
        doThrow(new LinkAlreadyTrackedException()).when(linkTrackingService).addLink(anyLong(), any(), any());

        SendMessage response = handler.handleInput(buildUpdate("тег1"));

        assertThat(text(response)).isEqualTo("Ссылка уже отслеживается.");
        verify(chatStateService).clearState(CHAT_ID);
    }

    @Test
    void unsupported_link_shows_error_and_clears_state() {
        URI url = URI.create("https://github.com/foo/bar");
        when(chatStateService.getPendingUrl(CHAT_ID)).thenReturn(url);
        doThrow(new UnsupportedLinkException()).when(linkTrackingService).addLink(anyLong(), any(), any());

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
