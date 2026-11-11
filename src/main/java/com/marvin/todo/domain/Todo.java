package com.marvin.todo.domain;

import java.time.LocalDate;

public record Todo(long id, String title, String description, LocalDate dueDate, boolean completed) {

    public Todo {
        if (id < 1) {
            throw new IllegalArgumentException("id는 1 이상이어야 한다: " + id);
        }
        TextRules.requireNormalizedTitle(title);
        TextRules.requireNormalizedDescription(description);
    }

    public boolean isOverdue(LocalDate today) {
        return !completed && dueDate != null && dueDate.isBefore(today);
    }

    public Todo apply(TodoUpdate update) {
        return new Todo(id, update.title(), update.description(), update.dueDate(), update.completed());
    }

    public Todo withCompleted(boolean completed) {
        return new Todo(id, title, description, dueDate, completed);
    }
}
