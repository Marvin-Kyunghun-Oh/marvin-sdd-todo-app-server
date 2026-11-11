package com.marvin.todo.presentation;

import com.marvin.todo.application.CreateTodoCommand;
import com.marvin.todo.application.TodoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/todos")
class TodoController {

    private final TodoService service;

    TodoController(TodoService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TodoResponse create(@RequestBody CreateTodoRequest request) {
        return TodoResponse.from(service.create(new CreateTodoCommand(request.title())));
    }

    @GetMapping
    TodoListResponse list(@RequestParam(name = "status", required = false) String status) {
        return TodoListResponse.from(service.list(StatusParam.parse(status)));
    }

    @GetMapping("/{id}")
    TodoResponse get(@PathVariable("id") String id) {
        return TodoResponse.from(service.get(PathId.parse(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable("id") String id) {
        service.delete(PathId.parse(id));
    }

    @PutMapping("/{id}")
    TodoResponse update(@PathVariable("id") String id, @RequestBody UpdateTodoRequest request) {
        return TodoResponse.from(service.update(PathId.parse(id), request.toCommand()));
    }
}
