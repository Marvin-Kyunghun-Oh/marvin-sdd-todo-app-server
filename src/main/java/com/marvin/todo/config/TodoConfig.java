package com.marvin.todo.config;

import java.time.Clock;
import java.time.ZoneId;

import com.marvin.todo.application.TodoService;
import com.marvin.todo.domain.TodoRepository;
import com.marvin.todo.persistence.InMemoryTodoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class TodoConfig {

    @Bean
    Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }

    @Bean
    TodoRepository todoRepository() {
        return new InMemoryTodoRepository();
    }

    @Bean
    TodoService todoService(TodoRepository repository, Clock clock) {
        return new TodoService(repository, clock);
    }
}
