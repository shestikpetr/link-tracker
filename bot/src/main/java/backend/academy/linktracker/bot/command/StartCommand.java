package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.service.LinkTrackingService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommand implements Command {
    private final LinkTrackingService linkTrackingService;

    @Override
    public int order() {
        return 0;
    }

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public String description() {
        return "Запуск бота";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        linkTrackingService.register(chatId);
        return new SendMessage(chatId, "Добро пожаловать! Используйте /help, чтобы посмотреть доступные команды.");
    }
}
