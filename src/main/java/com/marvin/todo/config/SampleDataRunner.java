package com.marvin.todo.config;

import java.time.Clock;
import java.time.LocalDate;

import com.marvin.todo.application.CreateTodoCommand;
import com.marvin.todo.application.TodoService;
import com.marvin.todo.application.UpdateTodoCommand;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
class SampleDataRunner implements ApplicationRunner {

    private final TodoService service;
    private final Clock clock;

    SampleDataRunner(TodoService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        LocalDate today = LocalDate.now(clock);
        add("PRD 초안 작성하기", today.minusDays(4), false);
        add("장보기", today, false);
        add("운동 가기", today.plusDays(3), false);
        add("책 읽기", null, false);
        add("영수증 정리", today.minusDays(2), true);
    }

    private void add(String title, LocalDate dueDate, boolean completed) {
        long id = service.create(new CreateTodoCommand(title)).todo().id();
        if (dueDate != null || completed) {
            String dueDateText = dueDate == null ? null : dueDate.toString();
            service.update(id, new UpdateTodoCommand(title, null, dueDateText, completed));
        }
    }
}
