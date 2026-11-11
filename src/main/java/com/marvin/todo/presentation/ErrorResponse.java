package com.marvin.todo.presentation;

public record ErrorResponse(Body error) {

    public record Body(String code, String message) {
    }

    static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new Body(code, message));
    }
}
