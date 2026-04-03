package backend.academy.linktracker.bot.command;

import backend.academy.linktracker.bot.state.ChatSession;
import backend.academy.linktracker.bot.state.ChatStateService;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UntrackCommand implements Command {
    private final ChatStateService chatStateService;

    @Override
    public int order() {
        return 4;
    }

    @Override
    public String command() {
        return "/untrack";
    }

    @Override
    public String description() {
        return "Прекратить отслеживание ссылки";
    }

    @Override
    public SendMessage handle(Update update) {
        long chatId = update.message().chat().id();
        chatStateService.setSession(chatId, new ChatSession.Untrack());
        return new SendMessage(chatId, "Введите URL:");
    }
}
