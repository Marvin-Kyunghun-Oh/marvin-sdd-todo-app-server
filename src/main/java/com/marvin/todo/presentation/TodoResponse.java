package com.marvin.todo.presentation;

import com.marvin.todo.application.TodoView;

public record TodoResponse(long id, String title, String description, String dueDate, boolean completed, boolean overdue) {

    static TodoResponse from(TodoView view) {
        var todo = view.todo();
        return new TodoResponse(
                todo.id(),
                todo.title(),
                todo.description(),
                todo.dueDate() == null ? null : todo.dueDate().toString(),
                todo.completed(),
                view.overdue());
    }
}
