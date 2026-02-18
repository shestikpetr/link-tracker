package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartCommandTest {

    private final StartCommand command = new StartCommand();

    @Test
    void command_name_is_start() {
        assertThat(command.command()).isEqualTo("/start");
    }

    @Test
    void description_is_not_blank() {
        assertThat(command.description()).isNotBlank();
    }

    @Test
    void handle_returns_send_message_to_correct_chat() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(42L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        var result = command.handle(update);

        assertThat(result).isNotNull();
        assertThat(result.getParameters()).containsEntry("chat_id", 42L);
    }

    @Test
    void handle_returns_send_message_with_welcome_text() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(1L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        var result = command.handle(update);

        assertThat(result.getParameters().get("text").toString())
                .contains("/help");
    }
}
