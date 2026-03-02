package backend.academy.linktracker.bot.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BotUpdateListenerTest {

    @Mock
    TelegramBot telegramBot;

    @Mock
    MessageHandler messageHandler;

    BotUpdateListener listener;

    @BeforeEach
    void setUp() {
        listener = new BotUpdateListener(telegramBot, messageHandler);
    }

    @Test
    void process_delegates_each_update_to_message_handler() {
        var update1 = mock(Update.class);
        var update2 = mock(Update.class);

        listener.process(List.of(update1, update2));

        verify(messageHandler).handle(update1);
        verify(messageHandler).handle(update2);
    }

    @Test
    void process_empty_list_returns_confirmed_all() {
        int result = listener.process(List.of());

        assertThat(result).isEqualTo(UpdatesListener.CONFIRMED_UPDATES_ALL);
    }

    @Test
    void process_returns_confirmed_all() {
        var update = mock(Update.class);

        int result = listener.process(List.of(update));

        assertThat(result).isEqualTo(UpdatesListener.CONFIRMED_UPDATES_ALL);
    }
}
