package backend.academy.linktracker.bot.command;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HelpCommandTest {

    @Mock
    ApplicationContext context;

    HelpCommand command;

    @BeforeEach
    void setUp() {
        command = new HelpCommand(context);
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
        var startCommand = new StartCommand();
        when(context.getBeansOfType(Command.class)).thenReturn(Map.of("startCommand", startCommand));

        var chat = mock(Chat.class);
        when(chat.id()).thenReturn(1L);
        var message = mock(Message.class);
        when(message.chat()).thenReturn(chat);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);

        var result = command.handle(update);

        assertThat(result.getParameters().get("text").toString())
                .contains("/start")
                .contains(startCommand.description());
    }

    @Test
    void handle_returns_send_message_to_correct_chat() {
        when(context.getBeansOfType(Command.class)).thenReturn(Map.of());

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
