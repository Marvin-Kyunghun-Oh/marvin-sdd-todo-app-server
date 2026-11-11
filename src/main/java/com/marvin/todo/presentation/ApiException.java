package com.marvin.todo.presentation;

class ApiException extends RuntimeException {

    private final ApiError error;

    ApiException(ApiError error) {
        super(error.name());
        this.error = error;
    }

    ApiError error() {
        return error;
    }
}
