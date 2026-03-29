package backend.academy.linktracker.bot.client;

import backend.academy.linktracker.bot.dto.ApiErrorResponse;
import backend.academy.linktracker.bot.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.bot.exceptions.ChatNotFoundException;
import backend.academy.linktracker.bot.exceptions.LinkAlreadyTrackedException;
import backend.academy.linktracker.bot.exceptions.LinkNotFoundException;
import backend.academy.linktracker.bot.exceptions.UnsupportedLinkException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class ScrapperErrorHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Map<String, Function<String, RuntimeException>> EXCEPTION_MAP = Map.of(
            "ChatAlreadyExistsException", ChatAlreadyExistsException::new,
            "ChatNotFoundException", ChatNotFoundException::new,
            "LinkAlreadyExistsException", LinkAlreadyTrackedException::new,
            "LinkNotFoundException", LinkNotFoundException::new,
            "UnsupportedLinkException", UnsupportedLinkException::new);

    public RuntimeException handle(InputStream body) {
        try {
            var error = OBJECT_MAPPER.readValue(body, ApiErrorResponse.class);
            var factory = EXCEPTION_MAP.get(error.exceptionName());
            if (factory != null) {
                return factory.apply(error.description());
            }
        } catch (IOException ignored) {
        }
        return new RuntimeException("Неизвестная ошибка");
    }
}
