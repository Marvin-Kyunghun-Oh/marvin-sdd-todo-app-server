package com.marvin.todo.domain;

import java.time.LocalDate;

public record Todo(long id, String title, String description, LocalDate dueDate, boolean completed) {

    public Todo {
        if (id < 1) {
            throw new IllegalArgumentException("id는 1 이상이어야 한다: " + id);
        }
        if (!TextRules.isNormalizedTitle(title)) {
            throw new IllegalArgumentException("제목은 공백이 제거된 1~100자여야 한다");
        }
        if (!TextRules.isNormalizedDescription(description)) {
            throw new IllegalArgumentException("설명은 없거나 공백이 제거된 1~1,000자여야 한다");
        }
    }

    public boolean isOverdue(LocalDate today) {
        return !completed && dueDate != null && dueDate.isBefore(today);
    }
}
