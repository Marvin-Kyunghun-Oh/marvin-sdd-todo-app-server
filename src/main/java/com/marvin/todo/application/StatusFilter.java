package com.marvin.todo.application;

import com.marvin.todo.domain.Todo;

public enum StatusFilter {
    ALL,
    ACTIVE,
    DONE;

    boolean matches(Todo todo) {
        return switch (this) {
            case ALL -> true;
            case ACTIVE -> !todo.completed();
            case DONE -> todo.completed();
        };
    }
}
