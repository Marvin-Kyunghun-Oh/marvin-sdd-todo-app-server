package com.marvin.todo.presentation;

import com.marvin.todo.application.UpdateTodoCommand;
import tools.jackson.databind.JsonNode;

public record UpdateTodoRequest(String title, String description, JsonNode dueDate, Boolean completed) {

    UpdateTodoCommand toCommand() {
        if (completed == null) {
            throw new ApiException(ApiError.MALFORMED_REQUEST);
        }
        return new UpdateTodoCommand(title, description, dueDateText(), completed);
    }

    private String dueDateText() {
        if (dueDate == null || dueDate.isNull()) {
            return null;
        }
        return dueDate.isString() ? dueDate.asString() : dueDate.toString();
    }
}
