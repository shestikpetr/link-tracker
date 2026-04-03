package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ChatAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChatAlreadyExists(ChatAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatusCode.valueOf(409), "Чат уже зарегистрирован");
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFound(ChatNotFoundException ex) {
        return buildResponse(ex, HttpStatusCode.valueOf(404), "Чат не найден");
    }

    @ExceptionHandler(LinkAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkAlreadyExists(LinkAlreadyExistsException ex) {
        return buildResponse(ex, HttpStatusCode.valueOf(409), "Ссылка уже отслеживается");
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkNotFound(LinkNotFoundException ex) {
        return buildResponse(ex, HttpStatusCode.valueOf(404), "Ссылка не найдена");
    }

    @ExceptionHandler(UnsupportedLinkException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedLink(UnsupportedLinkException ex) {
        return buildResponse(ex, HttpStatusCode.valueOf(422), "Ссылка не поддерживается");
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            RuntimeException ex, HttpStatusCode status, String description) {
        log.atWarn()
                .setMessage("Обработка исключения")
                .addKeyValue("description", description)
                .addKeyValue("error", ex.getMessage())
                .log();
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(
                        description,
                        String.valueOf(status.value()),
                        ex.getClass().getSimpleName(),
                        ex.getMessage(),
                        List.of()));
    }
}
