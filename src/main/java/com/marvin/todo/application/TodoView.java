package com.marvin.todo.application;

import com.marvin.todo.domain.Todo;

public record TodoView(Todo todo, boolean overdue) {
}
