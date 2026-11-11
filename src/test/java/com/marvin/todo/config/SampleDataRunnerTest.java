package com.marvin.todo.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.marvin.todo.application.CreateTodoCommand;
import com.marvin.todo.application.TodoService;
import com.marvin.todo.application.TodoView;
import com.marvin.todo.application.UpdateTodoCommand;
import com.marvin.todo.domain.Todo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SampleDataRunnerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private TodoService service;

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong();
        when(service.create(any())).thenAnswer(invocation -> {
            CreateTodoCommand command = invocation.getArgument(0);
            return new TodoView(new Todo(ids.incrementAndGet(), command.title(), null, null, false), false);
        });
    }

    @Test
    @DisplayName("샘플 Runner는 `TodoService`를 통해 할 일을 만든다 (TRD 7장)")
    void 서비스를_통해_만든다() {
        new SampleDataRunner(service, CLOCK).run(null);

        ArgumentCaptor<CreateTodoCommand> created = ArgumentCaptor.forClass(CreateTodoCommand.class);
        verify(service, atLeastOnce()).create(created.capture());
        assertThat(created.getAllValues()).hasSize(5);
    }

    @Test
    @DisplayName("샘플에는 기한 지남, 오늘 마감, 미래 마감, 마감 없음, 완료 항목이 모두 포함된다 (TRD 7장)")
    void 다섯_경우가_모두_포함된다() {
        new SampleDataRunner(service, CLOCK).run(null);

        ArgumentCaptor<UpdateTodoCommand> updates = ArgumentCaptor.forClass(UpdateTodoCommand.class);
        verify(service, atLeastOnce()).update(anyLong(), updates.capture());
        List<UpdateTodoCommand> commands = updates.getAllValues();

        assertThat(commands).extracting(UpdateTodoCommand::dueDateText)
                .contains("2026-11-06", "2026-11-10", "2026-11-13");
        assertThat(commands).anyMatch(UpdateTodoCommand::completed);
        assertThat(commands).hasSize(4);
    }
}
