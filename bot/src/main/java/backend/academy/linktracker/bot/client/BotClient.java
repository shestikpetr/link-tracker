package backend.academy.linktracker.bot.client;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.response.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotClient {
    private final TelegramBot telegramBot;

    public <T extends BaseRequest<T, R>, R extends BaseResponse> void execute(BaseRequest<T, R> request) {
        R response = telegramBot.execute(request);
        if (response.isOk()) {
            log.atDebug()
                    .setMessage("Запрос выполнен успешно")
                    .addKeyValue("method", request.getMethod())
                    .log();
        } else {
            log.atError()
                    .setMessage("Ошибка запроса")
                    .addKeyValue("method", request.getMethod())
                    .addKeyValue("error", response.description())
                    .log();
        }
    }
}
