package com.marvin.todo.application;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.stream.Stream;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoException;
import com.marvin.todo.domain.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T03:00:00Z"), SEOUL);

    @Mock
    private TodoRepository repository;

    private TodoService service;

    @BeforeEach
    void setUp() {
        service = new TodoService(repository, CLOCK);
    }

    @Test
    @DisplayName("AC-01-1 할 일이 하나도 없을 때 제목 \"장보기\"로 추가하면 제목 \"장보기\", 설명 없음, 마감일 없음, 진행 중인 할 일이 새로 만들어진다")
    void 제목만으로_추가하면_설명_없음_마감일_없음_진행_중이다() {
        when(repository.create(new NewTodo("장보기"))).thenReturn(new Todo(1L, "장보기", null, null, false));

        TodoView view = service.create(new CreateTodoCommand("장보기"));

        assertThat(view.todo()).isEqualTo(new Todo(1L, "장보기", null, null, false));
        assertThat(view.overdue()).isFalse();
    }

    @Test
    @DisplayName("AC-01-2 같은 제목의 할 일이 이미 있을 때 같은 제목으로 추가하면 새 할 일이 별도로 만들어진다")
    void 같은_제목으로_추가해도_별도로_만들어진다() {
        when(repository.create(new NewTodo("장보기")))
                .thenReturn(new Todo(1L, "장보기", null, null, false))
                .thenReturn(new Todo(2L, "장보기", null, null, false));

        TodoView first = service.create(new CreateTodoCommand("장보기"));
        TodoView second = service.create(new CreateTodoCommand("장보기"));

        assertThat(second.todo().id()).isNotEqualTo(first.todo().id());
        verify(repository, times(2)).create(new NewTodo("장보기"));
    }

    @Test
    @DisplayName("AC-01-3 제목 앞뒤에 공백이 있을 때 추가하면 공백이 제거된 제목으로 저장된다")
    void 제목_앞뒤_공백이_제거된_제목이_저장소에_넘어간다() {
        when(repository.create(new NewTodo("장보기"))).thenReturn(new Todo(1L, "장보기", null, null, false));

        service.create(new CreateTodoCommand("  장보기\t"));

        verify(repository).create(new NewTodo("장보기"));
    }

    static Stream<String> 제목_오류가_나는_입력() {
        return Stream.of(null, "", "   ", "가".repeat(101));
    }

    @ParameterizedTest
    @MethodSource("제목_오류가_나는_입력")
    @DisplayName("AC-14-2 추가가 거부되었을 때 목록과 요약을 다시 보면 개수와 내용이 거부 이전과 같다")
    void 제목_오류로_거부되면_저장소는_호출되지_않는다(String title) {
        assertThatThrownBy(() -> service.create(new CreateTodoCommand(title)))
                .isInstanceOf(TodoException.class);

        verifyNoInteractions(repository);
    }
}
