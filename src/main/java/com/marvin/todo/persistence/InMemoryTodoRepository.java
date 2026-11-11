package com.marvin.todo.persistence;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.UnaryOperator;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoRepository;

public class InMemoryTodoRepository implements TodoRepository {

    private final ConcurrentSkipListMap<Long, Todo> store = new ConcurrentSkipListMap<>();
    private final AtomicLong lastId = new AtomicLong();

    @Override
    public Todo create(NewTodo newTodo) {
        Todo todo = new Todo(lastId.incrementAndGet(), newTodo.title(), null, null, false);
        store.put(todo.id(), todo);
        return todo;
    }

    @Override
    public Optional<Todo> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Todo> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public Optional<Todo> update(long id, UnaryOperator<Todo> change) {
        return Optional.ofNullable(store.computeIfPresent(id, (key, current) -> {
            Todo changed = change.apply(current);
            if (changed.id() != id) {
                throw new IllegalStateException("수정으로 번호를 바꿀 수 없다: " + id);
            }
            return changed;
        }));
    }

    @Override
    public boolean deleteById(long id) {
        return store.remove(id) != null;
    }
}
