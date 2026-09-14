package ru.yandex.practicum.telemetry.exception;

import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidation(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        return "Ошибка валидации запроса";
    }

    @ExceptionHandler(InvalidTypeIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleUnknownType(InvalidTypeIdException e) {
        log.warn("Неизвестный тип события: {}", e.getMessage());
        return "Неизвестный тип события";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Некорректные данные: {}", e.getMessage());
        return "Некорректные данные запроса";
    }
}