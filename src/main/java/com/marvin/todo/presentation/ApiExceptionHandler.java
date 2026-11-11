package com.marvin.todo.presentation;

import com.marvin.todo.domain.TodoException;
import org.apache.tomcat.util.http.InvalidParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TodoException.class)
    ResponseEntity<ErrorResponse> handleTodoException(TodoException exception) {
        return respond(ApiError.from(exception.code()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return respond(exception.error());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    ResponseEntity<ErrorResponse> handleUnreadable(Exception exception) {
        return respond(ApiError.MALFORMED_REQUEST);
    }

    // 이 API의 쿼리 파라미터는 `status` 하나라서, 인코딩이 깨져 읽을 수 없는 파라미터는 보기 구분 오류다 (TD-53).
    @ExceptionHandler(InvalidParameterException.class)
    ResponseEntity<ErrorResponse> handleUnreadableParameter(InvalidParameterException exception) {
        return respond(ApiError.INVALID_STATUS_FILTER);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleEndpointNotFound(NoResourceFoundException exception) {
        return respond(ApiError.ENDPOINT_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return respond(ApiError.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("처리되지 않은 예외", exception);
        return respond(ApiError.INTERNAL_ERROR);
    }

    private static ResponseEntity<ErrorResponse> respond(ApiError error) {
        return ResponseEntity.status(error.status())
                .contentType(MediaType.APPLICATION_JSON)
                .body(error.toResponse());
    }
}
