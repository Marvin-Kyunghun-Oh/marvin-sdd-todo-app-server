package com.marvin.todo.presentation;

import java.util.regex.Pattern;

final class PathId {

    private static final Pattern POSITIVE_INTEGER = Pattern.compile("[1-9][0-9]*");

    private PathId() {
    }

    static long parse(String text) {
        if (!POSITIVE_INTEGER.matcher(text).matches()) {
            throw new ApiException(ApiError.TODO_NOT_FOUND);
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ApiException(ApiError.TODO_NOT_FOUND);
        }
    }
}
