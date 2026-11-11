package com.marvin.todo.application;

public record UpdateTodoCommand(String title, String description, String dueDateText, boolean completed) {
}
