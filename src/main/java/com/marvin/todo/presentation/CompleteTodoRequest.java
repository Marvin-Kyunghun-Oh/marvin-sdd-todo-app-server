package com.marvin.todo.presentation;

public record CompleteTodoRequest(Boolean completed) {

    boolean requireCompleted() {
        if (completed == null) {
            throw new ApiException(ApiError.MALFORMED_REQUEST);
        }
        return completed;
    }
}
