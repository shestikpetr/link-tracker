package backend.academy.linktracker.bot.controller;

import backend.academy.linktracker.bot.client.BotClient;
import backend.academy.linktracker.bot.dto.LinkUpdate;
import com.pengrad.telegrambot.request.SendMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
public class UpdatesController {
    private final BotClient botClient;

    public UpdatesController(BotClient botClient) {
        this.botClient = botClient;
    }

    @PostMapping
    public void getUpdate(@RequestBody LinkUpdate update) {
        for (long chatId : update.tgChatIds()) {
            botClient.execute(new SendMessage(chatId, update.url() + "\n" + update.description()));
        }
    }
}
