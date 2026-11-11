package com.marvin.todo.domain;

final class TextRules {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;

    private TextRules() {
    }

    static String title(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.strip();
        if (title.isEmpty()) {
            throw new TodoException(TodoErrorCode.TITLE_REQUIRED);
        }
        if (length(title) > MAX_TITLE_LENGTH) {
            throw new TodoException(TodoErrorCode.TITLE_TOO_LONG);
        }
        return title;
    }

    static String description(String rawDescription) {
        if (rawDescription == null || rawDescription.isBlank()) {
            return null;
        }
        String description = rawDescription.strip();
        if (length(description) > MAX_DESCRIPTION_LENGTH) {
            throw new TodoException(TodoErrorCode.DESCRIPTION_TOO_LONG);
        }
        return description;
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
