package backend.academy.linktracker.bot.client;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BotClient {
    private final TelegramBot telegramBot;

    public BotClient(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public <T extends BaseRequest<T, R>, R extends BaseResponse> void execute(BaseRequest<T, R> request) {
        R response = telegramBot.execute(request);
        if (response.isOk()) {
            log.debug("Запрос {} выполнен успешно", request.getMethod());
        } else {
            log.error("Ошибка запроса {}: {}", request.getMethod(), response.description());
        }
    }
}
