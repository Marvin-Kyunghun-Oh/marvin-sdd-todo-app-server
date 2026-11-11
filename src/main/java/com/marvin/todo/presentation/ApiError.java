package com.marvin.todo.presentation;

import com.marvin.todo.domain.TodoErrorCode;
import org.springframework.http.HttpStatus;

enum ApiError {
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "제목을 입력해 주세요."),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "제목은 100자 이하로 입력해 주세요."),
    DESCRIPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "설명은 1,000자 이하로 입력해 주세요."),
    INVALID_DUE_DATE(HttpStatus.BAD_REQUEST, "마감일을 올바른 날짜로 입력해 주세요."),
    INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "보기 구분이 올바르지 않아요. 전체, 진행 중, 완료 중에서 골라 주세요."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "요청 내용을 읽을 수 없어요. 입력한 내용을 확인해 주세요."),
    TODO_NOT_FOUND(HttpStatus.NOT_FOUND, "할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요."),
    ENDPOINT_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없어요."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식이에요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String message;

    ApiError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    HttpStatus status() {
        return status;
    }

    ErrorResponse toResponse() {
        return ErrorResponse.of(name(), message);
    }

    static ApiError from(TodoErrorCode code) {
        return switch (code) {
            case TITLE_REQUIRED -> TITLE_REQUIRED;
            case TITLE_TOO_LONG -> TITLE_TOO_LONG;
            case DESCRIPTION_TOO_LONG -> DESCRIPTION_TOO_LONG;
            case INVALID_DUE_DATE -> INVALID_DUE_DATE;
            case TODO_NOT_FOUND -> TODO_NOT_FOUND;
        };
    }
}
