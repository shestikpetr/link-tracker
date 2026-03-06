package backend.academy.linktracker.scrapper.controller;

import backend.academy.linktracker.scrapper.dto.ApiErrorResponse;
import backend.academy.linktracker.scrapper.exceptions.ChatAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.ChatNotFoundException;
import backend.academy.linktracker.scrapper.exceptions.LinkAlreadyExistsException;
import backend.academy.linktracker.scrapper.exceptions.LinkNotFoundException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ChatAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleChatAlreadyExists(ChatAlreadyExistsException ex) {
        return new ApiErrorResponse(
                "Чат уже зарегистрирован", "409", ex.getClass().getSimpleName(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(ChatNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleChatNotFound(ChatNotFoundException ex) {
        return new ApiErrorResponse("Чат не найден", "404", ex.getClass().getSimpleName(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(LinkAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiErrorResponse handleLinkAlreadyExists(LinkAlreadyExistsException ex) {
        return new ApiErrorResponse(
                "Ссылка уже отслеживается", "409", ex.getClass().getSimpleName(), ex.getMessage(), List.of());
    }

    @ExceptionHandler(LinkNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorResponse handleLinkNotFound(LinkNotFoundException ex) {
        return new ApiErrorResponse(
                "Ссылка не найдена", "404", ex.getClass().getSimpleName(), ex.getMessage(), List.of());
    }
}
