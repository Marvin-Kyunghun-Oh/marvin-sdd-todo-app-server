package com.marvin.todo.application;

import java.util.List;

import com.marvin.todo.domain.Todo;

public record Summary(int total, int active, int done, int remaining) {

    static Summary of(List<Todo> todos) {
        int done = (int) todos.stream().filter(Todo::completed).count();
        int active = todos.size() - done;
        return new Summary(todos.size(), active, done, active);
    }
}
