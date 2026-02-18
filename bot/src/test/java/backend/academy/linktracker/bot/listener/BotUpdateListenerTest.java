package backend.academy.linktracker.bot.listener;

import backend.academy.linktracker.bot.command.StartCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
class BotUpdateListenerTest {

    @Mock
    TelegramBot telegramBot;

    BotUpdateListener listener;

    @BeforeEach
    void setUp() {
        listener = new BotUpdateListener(telegramBot, List.of(new StartCommand()));
    }

    @Test
    void process_known_command_executes_bot() {
        var update = buildUpdate("/start");

        listener.process(List.of(update));

        verify(telegramBot).execute(any(SendMessage.class));
    }

    @Test
    void process_unknown_command_sends_help_hint() {
        var update = buildUpdate("/unknown");

        listener.process(List.of(update));

        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(telegramBot).execute(captor.capture());
        assertThat(captor.getValue().getParameters().get("text").toString())
                .contains("/help");
    }

    @Test
    void process_null_text_skips_update() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(1L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(null);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        listener.process(List.of(update));

        verifyNoInteractions(telegramBot);
    }

    @Test
    void process_empty_text_skips_update() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(1L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn("");
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        listener.process(List.of(update));

        verifyNoInteractions(telegramBot);
    }

    @Test
    void process_empty_list_returns_confirmed_all() {
        int result = listener.process(List.of());

        assertThat(result).isEqualTo(UpdatesListener.CONFIRMED_UPDATES_ALL);
    }

    private Update buildUpdate(String text) {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(100L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        when(message.text()).thenReturn(text);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }
}
