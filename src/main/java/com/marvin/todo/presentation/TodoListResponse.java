package com.marvin.todo.presentation;

import java.util.List;

import com.marvin.todo.application.ListResult;

public record TodoListResponse(List<TodoResponse> items, SummaryResponse summary) {

    static TodoListResponse from(ListResult result) {
        return new TodoListResponse(
                result.items().stream().map(TodoResponse::from).toList(),
                SummaryResponse.from(result.summary()));
    }
}
