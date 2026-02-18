package backend.academy.linktracker.bot.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelpCommandTest {

    HelpCommand command;

    @BeforeEach
    void setUp() {
        command = new HelpCommand();
    }

    @Test
    void command_name_is_help() {
        assertThat(command.command()).isEqualTo("/help");
    }

    @Test
    void description_is_not_blank() {
        assertThat(command.description()).isNotBlank();
    }

    @Test
    void handle_lists_all_commands() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(1L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        var result = command.handle(update);

        String text = result.getParameters().get("text").toString();
        for (CommandInfo info : CommandInfo.values()) {
            assertThat(text).contains(info.command()).contains(info.description());
        }
    }

    @Test
    void handle_returns_send_message_to_correct_chat() {
        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(99L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        var result = command.handle(update);

        assertThat(result.getParameters()).containsEntry("chat_id", 99L);
    }
}
