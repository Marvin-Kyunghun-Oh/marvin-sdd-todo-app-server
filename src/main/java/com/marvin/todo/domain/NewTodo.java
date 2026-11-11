package com.marvin.todo.domain;

public record NewTodo(String title) {

    public NewTodo {
        TextRules.requireNormalizedTitle(title);
    }

    public static NewTodo of(String rawTitle) {
        return new NewTodo(TextRules.title(rawTitle));
    }
}
