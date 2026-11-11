package com.marvin.todo.application;

import java.time.Clock;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.TodoRepository;

public class TodoService {

    private final TodoRepository repository;
    private final Clock clock;

    public TodoService(TodoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public TodoView create(CreateTodoCommand command) {
        return new TodoView(repository.create(NewTodo.of(command.title())), false);
    }
}
