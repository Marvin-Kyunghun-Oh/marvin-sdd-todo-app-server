package com.marvin.todo.application;

import java.util.List;

public record ListResult(List<TodoView> items, Summary summary) {
}
