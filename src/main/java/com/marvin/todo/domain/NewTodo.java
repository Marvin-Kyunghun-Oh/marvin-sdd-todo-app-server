package com.marvin.todo.domain;

public record NewTodo(String title) {

    private static final int MAX_TITLE_LENGTH = 100;

    public static NewTodo of(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.strip();
        if (title.isEmpty()) {
            throw new TodoException(TodoErrorCode.TITLE_REQUIRED);
        }
        if (title.codePointCount(0, title.length()) > MAX_TITLE_LENGTH) {
            throw new TodoException(TodoErrorCode.TITLE_TOO_LONG);
        }
        return new NewTodo(title);
    }
}
