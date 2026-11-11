package com.marvin.todo.domain;

public class TodoException extends RuntimeException {

    private final TodoErrorCode code;

    public TodoException(TodoErrorCode code) {
        super(code.name());
        this.code = code;
    }

    public TodoErrorCode code() {
        return code;
    }
}
