package backend.academy.linktracker.bot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BotUpdateListener implements UpdatesListener {
    private final TelegramBot telegramBot;
    private final MessageHandler messageHandler;

    public BotUpdateListener(TelegramBot telegramBot, MessageHandler messageHandler) {
        this.telegramBot = telegramBot;
        this.messageHandler = messageHandler;
    }

    @Override
    public int process(List<Update> list) {
        list.forEach(messageHandler::handle);
        return CONFIRMED_UPDATES_ALL;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }
}
