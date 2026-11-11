package com.marvin.todo.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoErrorCode;
import com.marvin.todo.domain.TodoException;
import com.marvin.todo.domain.TodoUpdate;
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
        LocalDate today = LocalDate.now(clock);
        List<Todo> snapshot = repository.findAll();
        List<TodoView> items = snapshot.stream()
                .filter(filter::matches)
                .map(todo -> new TodoView(todo, todo.isOverdue(today)))
                .toList();
        return new ListResult(items, Summary.of(snapshot));
    }

    public TodoView get(long id) {
        Todo todo = repository.findById(id).orElseThrow(TodoService::notFound);
        return new TodoView(todo, todo.isOverdue(LocalDate.now(clock)));
    }

    public void delete(long id) {
        if (!repository.deleteById(id)) {
            throw notFound();
        }
    }

    public TodoView update(long id, UpdateTodoCommand command) {
        repository.findById(id).orElseThrow(TodoService::notFound);
        TodoUpdate update = TodoUpdate.of(command.title(), command.description(), command.dueDateText(), command.completed());
        Todo updated = repository.update(id, current -> current.apply(update))
                .orElseThrow(TodoService::notFound);
        return new TodoView(updated, updated.isOverdue(LocalDate.now(clock)));
    }

    private static TodoException notFound() {
        return new TodoException(TodoErrorCode.TODO_NOT_FOUND);
    }
}
