package com.marvin.todo.domain;

import java.time.LocalDate;

public record TodoUpdate(String title, String description, LocalDate dueDate, boolean completed) {

    public TodoUpdate {
        TextRules.requireNormalizedTitle(title);
        TextRules.requireNormalizedDescription(description);
    }

    public static TodoUpdate of(String rawTitle, String rawDescription, String rawDueDateText, boolean completed) {
        String title = TextRules.title(rawTitle);
        String description = TextRules.description(rawDescription);
        LocalDate dueDate = TextRules.dueDate(rawDueDateText);
        return new TodoUpdate(title, description, dueDate, completed);
    }
}
