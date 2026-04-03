package backend.academy.linktracker.bot.service;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LinkUpdateNotifier {
    private final BotClient botClient;

    public void notify(LinkUpdate update) {
        for (long chatId : update.tgChatIds()) {
            botClient.execute(new SendMessage(chatId, update.url() + "\n" + update.description()));
        }
    }
}
