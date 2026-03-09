package backend.academy.linktracker.bot.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageHandlerTest {
    @Mock
    CommandDispatcher commandDispatcher;

    @Mock
    StateInputDispatcher stateInputDispatcher;

    @Mock
    ChatStateService chatStateService;

    MessageHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageHandler(commandDispatcher, stateInputDispatcher, chatStateService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void handle_blank_text_skips_update(String text) {
        handler.handle(buildUpdate(text));
        verifyNoInteractions(commandDispatcher, stateInputDispatcher);
    }

    private Update buildUpdate(String text) {
        var message = mock(Message.class);
        when(message.text()).thenReturn(text);
        var update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }
}
