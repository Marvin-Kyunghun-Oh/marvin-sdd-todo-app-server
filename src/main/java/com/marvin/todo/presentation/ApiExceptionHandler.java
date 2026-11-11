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
        ApiError error = ApiError.from(exception.code());
        return ResponseEntity.status(error.status()).body(error.toResponse());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(ApiError.MALFORMED_REQUEST.status()).body(ApiError.MALFORMED_REQUEST.toResponse());
    }
}
