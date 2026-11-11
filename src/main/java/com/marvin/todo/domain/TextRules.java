package com.marvin.todo.domain;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

final class TextRules {

    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final Pattern DUE_DATE_FORMAT = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");

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

    static LocalDate dueDate(String rawDueDateText) {
        if (rawDueDateText == null || rawDueDateText.isBlank()) {
            return null;
        }
        String text = rawDueDateText.strip();
        if (!DUE_DATE_FORMAT.matcher(text).matches()) {
            throw new TodoException(TodoErrorCode.INVALID_DUE_DATE);
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new TodoException(TodoErrorCode.INVALID_DUE_DATE);
        }
    }

    static boolean isNormalizedTitle(String title) {
        return title != null && !title.isEmpty() && title.equals(title.strip()) && length(title) <= MAX_TITLE_LENGTH;
    }

    static boolean isNormalizedDescription(String description) {
        return description == null
                || (!description.isEmpty() && description.equals(description.strip()) && length(description) <= MAX_DESCRIPTION_LENGTH);
    }

    private static int length(String text) {
        return text.codePointCount(0, text.length());
    }
}
