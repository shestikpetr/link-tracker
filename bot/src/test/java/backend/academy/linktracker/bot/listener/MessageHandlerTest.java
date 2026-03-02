package backend.academy.linktracker.bot.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.command.StartCommand;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageHandlerTest {

    @Mock
    BotClient botClient;

    MessageHandler handler;

    @BeforeEach
    void setUp() {
        CommandRegistry registry = new CommandRegistry(List.of(new StartCommand()));
        handler = new MessageHandler(registry, botClient);
    }

    @Test
    void handle_known_command_executes_bot() {
        var update = buildUpdate("/start");

        handler.handle(update);

        verify(botClient).execute(any(SendMessage.class));
    }

    @Test
    void handle_unknown_command_sends_help_hint() {
        var update = buildUpdate("/unknown");

        handler.handle(update);

        var captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botClient).execute(captor.capture());
        assertThat(captor.getValue().getParameters().get("text").toString()).contains("/help");
    }

    @Test
    void handle_null_text_skips_update() {
        handler.handle(buildUpdate(null));

        verifyNoInteractions(botClient);
    }

    @Test
    void handle_empty_text_skips_update() {
        handler.handle(buildUpdate(""));

        verifyNoInteractions(botClient);
    }

    private Update buildUpdate(String text) {
        var message = mock(Message.class);
        when(message.text()).thenReturn(text);
        if (text != null && !text.isEmpty()) {
            var chat = mock(Chat.class);
            when(chat.id()).thenReturn(100L);
            when(message.chat()).thenReturn(chat);
        }
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }
}
