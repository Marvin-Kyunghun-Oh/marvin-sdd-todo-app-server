package com.marvin.todo.presentation;

import com.marvin.todo.domain.TodoException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(TodoException.class)
    ResponseEntity<ErrorResponse> handleTodoException(TodoException exception) {
        return respond(ApiError.from(exception.code()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return respond(exception.error());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return respond(ApiError.MALFORMED_REQUEST);
    }

    private static ResponseEntity<ErrorResponse> respond(ApiError error) {
        return ResponseEntity.status(error.status()).body(error.toResponse());
    }
}
