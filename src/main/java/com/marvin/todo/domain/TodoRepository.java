package com.marvin.todo.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface TodoRepository {

    Todo create(NewTodo newTodo);

    Optional<Todo> findById(long id);

    List<Todo> findAll();

    Optional<Todo> update(long id, UnaryOperator<Todo> change);

    boolean deleteById(long id);
}
