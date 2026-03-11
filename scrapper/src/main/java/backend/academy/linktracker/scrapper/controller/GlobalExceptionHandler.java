package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.UnsupportedLinkException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleChatAlreadyExists(ChatAlreadyExistsException ex) {
        log.warn("Чат уже зарегистрирован: {}", ex.getMessage());
        return ResponseEntity.status(409)
                .body(new ApiErrorResponse(
                        "Чат уже зарегистрирован", "409", ex.getClass().getSimpleName(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(ChatNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChatNotFound(ChatNotFoundException ex) {
        log.warn("Чат не найден: {}", ex.getMessage());
        return ResponseEntity.status(404)
                .body(new ApiErrorResponse(
                        "Чат не найден", "404", ex.getClass().getSimpleName(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(LinkAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkAlreadyExists(LinkAlreadyExistsException ex) {
        log.warn("Ссылка уже отслеживается: {}", ex.getMessage());
        return ResponseEntity.status(409)
                .body(new ApiErrorResponse(
                        "Ссылка уже отслеживается", "409", ex.getClass().getSimpleName(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(LinkNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleLinkNotFound(LinkNotFoundException ex) {
        log.warn("Ссылка не найдена: {}", ex.getMessage());
        return ResponseEntity.status(404)
                .body(new ApiErrorResponse(
                        "Ссылка не найдена", "404", ex.getClass().getSimpleName(), ex.getMessage(), List.of()));
    }

    @ExceptionHandler(UnsupportedLinkException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedLink(UnsupportedLinkException ex) {
        log.warn("Ссылка не поддерживается: {}", ex.getMessage());
        return ResponseEntity.status(422)
                .body(new ApiErrorResponse(
                        "Ссылка не поддерживается", "422", ex.getClass().getSimpleName(), ex.getMessage(), List.of()));
    }
}
