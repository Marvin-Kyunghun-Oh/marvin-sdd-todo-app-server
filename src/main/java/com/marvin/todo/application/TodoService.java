package com.marvin.todo.application;

import java.time.Clock;
import java.util.List;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
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

    public ListResult list(StatusFilter filter) {
        List<Todo> snapshot = repository.findAll();
        List<TodoView> items = snapshot.stream()
                .filter(filter::matches)
                .map(todo -> new TodoView(todo, false))
                .toList();
        return new ListResult(items, Summary.of(snapshot));
    }
}
