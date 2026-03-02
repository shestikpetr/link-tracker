package backend.academy.linktracker.bot.configuration;

import backend.academy.linktracker.bot.command.CommandRegistry;
import backend.academy.linktracker.bot.properties.TelegramProperties;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SetMyCommands;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TelegramConfiguration {

    @Bean
    public TelegramBot telegramBot(TelegramProperties properties, CommandRegistry commandRegistry) {
        var builder = new TelegramBot.Builder(properties.getToken())
                .apiUrl(properties.getUrl())
                .updateListenerSleep(properties.getUpdateListenerSleep().toMillis());

        if (properties.isDebug()) {
            builder.debug();
        }

        TelegramBot bot = builder.build();

        var response = bot.execute(new SetMyCommands(commandRegistry.toBotCommands()));
        if (!response.isOk()) {
            log.error("Ошибка регистрации команд: {}", response.description());
        }

        return bot;
    }
}
