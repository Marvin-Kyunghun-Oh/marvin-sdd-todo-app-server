package com.marvin.todo.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.marvin.todo.domain.NewTodo;
import com.marvin.todo.domain.Todo;
import com.marvin.todo.domain.TodoErrorCode;
import com.marvin.todo.domain.TodoException;
import com.marvin.todo.domain.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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

    @Test
    @DisplayName("AC-03-1 할 일 \"가\", \"나\", \"다\"가 이 순서로 등록되어 있을 때 목록을 보면 \"가\", \"나\", \"다\" 순서로 돌아온다")
    void 목록은_저장소가_준_순서를_유지한다() {
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "가", null, null, false),
                new Todo(2L, "나", null, null, false),
                new Todo(3L, "다", null, null, false)));

        ListResult result = service.list(StatusFilter.ALL);

        assertThat(result.items()).extracting(view -> view.todo().title()).containsExactly("가", "나", "다");
    }

    @Test
    @DisplayName("AC-03-2 마감일이나 완료 여부가 서로 다른 할 일들이 있을 때 목록을 보면 마감일이나 완료 여부와 관계없이 등록한 순서를 유지한다")
    void 마감일이나_완료_여부와_관계없이_순서를_바꾸지_않는다() {
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "가", null, LocalDate.of(2026, 12, 1), true),
                new Todo(2L, "나", null, LocalDate.of(2026, 11, 1), false),
                new Todo(3L, "다", null, null, true)));

        ListResult result = service.list(StatusFilter.ALL);

        assertThat(result.items()).extracting(view -> view.todo().id()).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("AC-03-3 목록을 보면 각 할 일에 대해 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 함께 돌아온다")
    void 각_항목에_다섯_값이_담긴다() {
        Todo todo = new Todo(1L, "가", "설명", LocalDate.of(2026, 11, 12), false);
        when(repository.findAll()).thenReturn(List.of(todo));

        TodoView view = service.list(StatusFilter.ALL).items().get(0);

        assertThat(view.todo()).isEqualTo(todo);
        assertThat(view.overdue()).isFalse();
    }

    @Test
    @DisplayName("AC-09-3 마감일이 없는 할 일이 있을 때 목록을 보면 마감일 없음으로 돌아온다")
    void 마감일이_없으면_null로_돌아온다() {
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, null, false)));

        assertThat(service.list(StatusFilter.ALL).items().get(0).todo().dueDate()).isNull();
    }

    private static List<Todo> 진행_중_3개_완료_1개() {
        return List.of(
                new Todo(1L, "가", null, null, false),
                new Todo(2L, "나", null, null, false),
                new Todo(3L, "다", null, null, true),
                new Todo(4L, "라", null, null, false));
    }

    @Test
    @DisplayName("AC-04-3 진행 중 3개, 완료 1개가 있을 때 전체를 보면 4개가 모두 돌아온다")
    void 전체_보기는_모두_돌려준다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.ALL).items()).hasSize(4);
    }

    @Test
    @DisplayName("AC-04-1 진행 중 3개, 완료 1개가 있을 때 진행 중만 보면 진행 중 3개만 등록 순서대로 돌아온다")
    void 진행_중_보기는_진행_중만_순서대로_돌려준다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.ACTIVE).items())
                .extracting(view -> view.todo().title()).containsExactly("가", "나", "라");
    }

    @Test
    @DisplayName("AC-04-2 진행 중 3개, 완료 1개가 있을 때 완료만 보면 완료 1개만 돌아온다")
    void 완료_보기는_완료만_돌려준다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.DONE).items())
                .extracting(view -> view.todo().title()).containsExactly("다");
    }

    @Test
    @DisplayName("AC-04-4 완료된 할 일이 하나도 없을 때 완료만 보면 오류 없이 빈 목록이 돌아온다")
    void 완료가_없으면_완료_보기는_빈_목록이다() {
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, null, false)));

        assertThat(service.list(StatusFilter.DONE).items()).isEmpty();
    }

    @Test
    @DisplayName("AC-05-1 진행 중 3개, 완료 1개가 있을 때 요약을 보면 전체 4, 진행 중 3, 완료 1, 남은 개수 3이 돌아온다")
    void 요약은_전체_진행_중_완료_남은_개수를_센다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.ALL).summary()).isEqualTo(new Summary(4, 3, 1, 3));
    }

    @Test
    @DisplayName("AC-05-2 진행 중 3개, 완료 1개가 있을 때 완료만 보면서 요약을 함께 받으면 요약 값은 위와 같고, 보기 선택에 영향을 받지 않는다")
    void 요약은_보기_선택에_영향을_받지_않는다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        assertThat(service.list(StatusFilter.DONE).summary()).isEqualTo(new Summary(4, 3, 1, 3));
        assertThat(service.list(StatusFilter.ACTIVE).summary()).isEqualTo(new Summary(4, 3, 1, 3));
    }

    @Test
    @DisplayName("AC-05-3 할 일이 하나도 없을 때 요약을 보면 전체 0, 진행 중 0, 완료 0, 남은 개수 0이 돌아온다")
    void 할_일이_없으면_요약은_모두_0이다() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.list(StatusFilter.ALL).summary()).isEqualTo(new Summary(0, 0, 0, 0));
    }

    @Test
    @DisplayName("AC-13-1 할 일이 하나도 없을 때 목록을 보면 오류 없이 빈 목록과 모든 개수 0이 돌아온다")
    void 할_일이_없으면_빈_목록과_0_요약이_돌아온다() {
        when(repository.findAll()).thenReturn(List.of());

        ListResult result = service.list(StatusFilter.ALL);

        assertThat(result.items()).isEmpty();
        assertThat(result.summary()).isEqualTo(new Summary(0, 0, 0, 0));
    }

    @Test
    @DisplayName("목록과 요약은 한 번 조회한 결과에서 함께 계산한다 (TRD 3.1)")
    void 목록과_요약은_저장소를_한_번만_조회한다() {
        when(repository.findAll()).thenReturn(진행_중_3개_완료_1개());

        service.list(StatusFilter.DONE);

        verify(repository, times(1)).findAll();
    }

    private boolean 목록에서_기한_지남인가(boolean completed, LocalDate dueDate) {
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, dueDate, completed)));

        return service.list(StatusFilter.ALL).items().get(0).overdue();
    }

    @Test
    @DisplayName("AC-10-1 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-06인 할 일이 있을 때 목록을 보면 그 할 일은 기한 지남이다")
    void 마감일이_오늘보다_이전이면_기한_지남이다() {
        assertThat(목록에서_기한_지남인가(false, LocalDate.of(2026, 11, 6))).isTrue();
    }

    @Test
    @DisplayName("AC-10-2 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-10인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 마감일이_오늘이면_기한_지남이_아니다() {
        assertThat(목록에서_기한_지남인가(false, LocalDate.of(2026, 11, 10))).isFalse();
    }

    @Test
    @DisplayName("AC-10-3 오늘이 2026-11-10이고, 진행 중이며 마감일이 2026-11-12인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 마감일이_오늘보다_이후면_기한_지남이_아니다() {
        assertThat(목록에서_기한_지남인가(false, LocalDate.of(2026, 11, 12))).isFalse();
    }

    @Test
    @DisplayName("AC-10-4 오늘이 2026-11-10이고, 완료되었으며 마감일이 2026-11-09인 할 일이 있을 때 목록을 보면 기한 지남이 아니다")
    void 완료된_할_일은_기한_지남이_아니다() {
        assertThat(목록에서_기한_지남인가(true, LocalDate.of(2026, 11, 9))).isFalse();
    }

    @Test
    @DisplayName("AC-10-6 한국 시간으로 2026-11-10 00:30(세계 표준시로는 11-09)일 때 마감일 2026-11-09인 진행 중 할 일의 기한 지남을 판정하면 오늘은 2026-11-10으로 계산되어 기한 지남이다")
    void 한국_날짜_기준으로_오늘을_계산한다() {
        Clock lateNightUtc = Clock.fixed(Instant.parse("2026-11-09T15:30:00Z"), SEOUL);
        TodoService seoulService = new TodoService(repository, lateNightUtc);
        when(repository.findAll()).thenReturn(List.of(new Todo(1L, "가", null, LocalDate.of(2026, 11, 9), false)));

        assertThat(seoulService.list(StatusFilter.ALL).items().get(0).overdue()).isTrue();
    }

    @Nested
    @DisplayName("기한 지남 여부가 섞인 목록")
    class MixedOverdueList {

        @Test
        @DisplayName("AC-03-3 목록을 보면 각 할 일에 대해 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 함께 돌아온다")
        void 섞인_목록에서_항목마다_기한_지남_여부가_맞다() {
            when(repository.findAll()).thenReturn(List.of(
                    new Todo(1L, "지남", null, LocalDate.of(2026, 11, 6), false),
                    new Todo(2L, "오늘", null, LocalDate.of(2026, 11, 10), false),
                    new Todo(3L, "완료", null, LocalDate.of(2026, 11, 9), true),
                    new Todo(4L, "없음", null, null, false)));

            assertThat(service.list(StatusFilter.ALL).items())
                    .extracting(TodoView::overdue).containsExactly(true, false, false, false);
        }
    }

    @Test
    @DisplayName("목록의 모든 항목은 같은 \"오늘\"로 판정한다 (DATA_MODEL 3.1)")
    void 오늘은_요청당_한_번만_계산한다() {
        AtomicInteger instantCalls = new AtomicInteger();
        Clock countingClock = new Clock() {
            @Override
            public ZoneId getZone() {
                return SEOUL;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                instantCalls.incrementAndGet();
                return CLOCK.instant();
            }
        };
        TodoService countingService = new TodoService(repository, countingClock);
        when(repository.findAll()).thenReturn(List.of(
                new Todo(1L, "가", null, LocalDate.of(2026, 11, 6), false),
                new Todo(2L, "나", null, LocalDate.of(2026, 11, 7), false),
                new Todo(3L, "다", null, LocalDate.of(2026, 11, 8), false)));

        countingService.list(StatusFilter.ALL);

        assertThat(instantCalls).hasValue(1);
    }

    @Nested
    @DisplayName("한 건 보기")
    class Get {

        @Test
        @DisplayName("AC-07-1 할 일이 있을 때 그 할 일을 열면 제목, 설명, 마감일, 완료 여부, 기한 지남 여부가 돌아온다")
        void 있는_할_일을_열면_다섯_값이_돌아온다() {
            Todo todo = new Todo(1L, "가", "설명", LocalDate.of(2026, 11, 6), false);
            when(repository.findById(1L)).thenReturn(Optional.of(todo));

            TodoView view = service.get(1L);

            assertThat(view.todo()).isEqualTo(todo);
            assertThat(view.overdue()).isTrue();
        }

        @Test
        @DisplayName("AC-12-1 한 번도 만들어진 적 없는 할 일을 가리킬 때 열면 \"할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요.\"라는 안내가 돌아온다")
        void 없는_할_일을_열면_할_일_없음_오류() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(99L))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));
        }

        @Test
        @DisplayName("AC-11-2 방금 삭제한 할 일을 가리킬 때 다시 열면 \"할 일을 찾을 수 없어요. 이미 삭제되었을 수 있어요.\"라는 안내가 돌아온다")
        void 삭제한_뒤_열면_할_일_없음_오류() {
            when(repository.deleteById(1L)).thenReturn(true);
            when(repository.findById(1L)).thenReturn(Optional.empty());

            service.delete(1L);

            assertThatThrownBy(() -> service.get(1L))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("AC-11-1 할 일이 있을 때 삭제하면 그 할 일은 이후 목록과 요약에서 사라진다")
        void 삭제하면_저장소_삭제를_호출한다() {
            when(repository.deleteById(1L)).thenReturn(true);

            service.delete(1L);

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("AC-12-2 한 번도 만들어진 적 없는 할 일을 가리킬 때 삭제를 시도하면 같은 안내가 돌아오고, 다른 할 일은 바뀌지 않는다")
        void 없는_할_일을_삭제하면_할_일_없음_오류이고_다른_호출은_없다() {
            when(repository.deleteById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));

            verify(repository).deleteById(99L);
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        private final Todo existing = new Todo(1L, "PRD 초안", null, null, false);

        private void 기존_할_일이_있다(Todo todo) {
            when(repository.findById(todo.id())).thenReturn(Optional.of(todo));
        }

        @SuppressWarnings("unchecked")
        private void 교체하면_변경이_적용된다(Todo todo) {
            when(repository.update(eq(todo.id()), any())).thenAnswer(invocation ->
                    Optional.of(((UnaryOperator<Todo>) invocation.getArgument(1)).apply(todo)));
        }

        private TodoView 수정한다(String title, String description, String dueDateText, boolean completed) {
            기존_할_일이_있다(existing);
            교체하면_변경이_적용된다(existing);
            return service.update(1L, new UpdateTodoCommand(title, description, dueDateText, completed));
        }

        @Test
        @DisplayName("AC-08-1 제목 \"PRD 초안\", 마감일 없음인 할 일이 있을 때 제목을 \"PRD 초안 작성하기\", 설명을 \"목적과 범위 정리\", 마감일을 2026-11-12로 저장하면 세 값이 모두 반영되어 돌아온다")
        void 세_값이_모두_반영된다() {
            TodoView view = 수정한다("PRD 초안 작성하기", "목적과 범위 정리", "2026-11-12", false);

            assertThat(view.todo().title()).isEqualTo("PRD 초안 작성하기");
            assertThat(view.todo().description()).isEqualTo("목적과 범위 정리");
            assertThat(view.todo().dueDate()).isEqualTo(LocalDate.of(2026, 11, 12));
            assertThat(view.todo().id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("AC-08-2 진행 중인 할 일이 있을 때 수정에서 완료로 표시하여 저장하면 완료 상태가 된다")
        void 완료로_표시하여_저장하면_완료다() {
            assertThat(수정한다("PRD 초안", null, null, true).todo().completed()).isTrue();
        }

        @Test
        @DisplayName("AC-08-5 설명이 \"목적과 범위 정리\"인 할 일이 있을 때 설명을 비워서 저장하면 설명 없음이 된다")
        void 설명을_비우면_설명_없음이다() {
            Todo withDescription = new Todo(1L, "PRD 초안", "목적과 범위 정리", null, false);
            기존_할_일이_있다(withDescription);
            교체하면_변경이_적용된다(withDescription);

            TodoView view = service.update(1L, new UpdateTodoCommand("PRD 초안", "", null, false));

            assertThat(view.todo().description()).isNull();
        }

        @Test
        @DisplayName("AC-08-6 설명이 없는 할 일이 있을 때 설명을 공백 문자로만 채워 저장하면 설명 없음으로 저장된다")
        void 설명을_공백으로_채우면_설명_없음이다() {
            assertThat(수정한다("PRD 초안", "  \t　", null, false).todo().description()).isNull();
        }

        @Test
        @DisplayName("AC-09-1 마감일이 없는 할 일이 있을 때 마감일 2026-11-12를 지정하면 마감일이 2026-11-12로 저장된다")
        void 마감일을_지정하면_저장된다() {
            assertThat(수정한다("PRD 초안", null, "2026-11-12", false).todo().dueDate())
                    .isEqualTo(LocalDate.of(2026, 11, 12));
        }

        @Test
        @DisplayName("AC-09-2 마감일이 있는 할 일이 있을 때 마감일을 지우면 마감일 없음 상태가 되고, 기한 지남이 아니다")
        void 마감일을_지우면_마감일_없음이고_기한_지남이_아니다() {
            Todo withDueDate = new Todo(1L, "PRD 초안", null, LocalDate.of(2026, 11, 1), false);
            기존_할_일이_있다(withDueDate);
            교체하면_변경이_적용된다(withDueDate);

            TodoView view = service.update(1L, new UpdateTodoCommand("PRD 초안", null, null, false));

            assertThat(view.todo().dueDate()).isNull();
            assertThat(view.overdue()).isFalse();
        }

        @Test
        @DisplayName("AC-09-5 오늘이 2026-11-10일 때 마감일을 2026-11-01로 지정하면 저장되고 기한 지남이다")
        void 과거_마감일도_저장되고_기한_지남이다() {
            TodoView view = 수정한다("PRD 초안", null, "2026-11-01", false);

            assertThat(view.todo().dueDate()).isEqualTo(LocalDate.of(2026, 11, 1));
            assertThat(view.overdue()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("AC-02-5 기존 할 일이 있을 때 제목을 비우거나 공백만 넣어 수정하면 수정은 거부되고 \"제목을 입력해 주세요.\"라는 안내가 돌아오며 기존 내용은 그대로다")
        void 제목을_비우면_제목_없음_오류이고_교체하지_않는다(String title) {
            기존_할_일이_있다(existing);

            assertThatThrownBy(() -> service.update(1L, new UpdateTodoCommand(title, null, null, false)))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TITLE_REQUIRED));

            verify(repository, never()).update(anyLong(), any());
        }

        static Stream<Arguments> 규칙에_어긋나는_수정() {
            return Stream.of(
                    Arguments.of("가".repeat(1001), null, TodoErrorCode.DESCRIPTION_TOO_LONG),
                    Arguments.of(null, "abc", TodoErrorCode.INVALID_DUE_DATE),
                    Arguments.of(null, "2026-02-30", TodoErrorCode.INVALID_DUE_DATE));
        }

        @ParameterizedTest
        @MethodSource("규칙에_어긋나는_수정")
        @DisplayName("AC-08-3 수정 내용이 규칙에 어긋날 때 저장하면 거부되고 기존 내용은 하나도 바뀌지 않는다")
        void 규칙에_어긋나면_교체하지_않는다(String description, String dueDateText, TodoErrorCode expected) {
            기존_할_일이_있다(existing);

            assertThatThrownBy(() -> service.update(1L, new UpdateTodoCommand("PRD 초안", description, dueDateText, false)))
                    .isInstanceOfSatisfying(TodoException.class, e -> assertThat(e.code()).isEqualTo(expected));

            verify(repository, never()).update(anyLong(), any());
        }

        @Test
        @DisplayName("AC-12-2 한 번도 만들어진 적 없는 할 일을 가리킬 때 수정을 시도하면 같은 안내가 돌아오고, 다른 할 일은 바뀌지 않는다")
        void 없는_할_일을_수정하면_할_일_없음_오류이고_교체하지_않는다() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, new UpdateTodoCommand("제목", null, null, false)))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));

            verify(repository, never()).update(anyLong(), any());
        }

        @Test
        @DisplayName("없는 할 일에 값 검증 오류가 있는 수정을 보내면 값 검증 오류가 아니라 \"할 일 없음\"이 먼저다 (TD-19)")
        void 없는_할_일이면_값_검증보다_할_일_없음이_먼저다() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(99L, new UpdateTodoCommand("", null, null, false)))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));
        }

        @Test
        @DisplayName("조회와 교체 사이에 삭제되어도 \"할 일 없음\"으로 처리한다 (DATA_MODEL 6)")
        void 교체가_빈_값이면_할_일_없음_오류() {
            기존_할_일이_있다(existing);
            when(repository.update(eq(1L), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(1L, new UpdateTodoCommand("제목", null, null, false)))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("완료 표시")
    class MarkCompleted {

        @SuppressWarnings("unchecked")
        private TodoView 표시한다(Todo todo, boolean completed) {
            when(repository.update(eq(todo.id()), any())).thenAnswer(invocation ->
                    Optional.of(((UnaryOperator<Todo>) invocation.getArgument(1)).apply(todo)));
            return service.markCompleted(todo.id(), completed);
        }

        @Test
        @DisplayName("AC-06-1 진행 중인 할 일이 있을 때 완료로 표시하면 그 할 일은 완료가 되고 요약의 진행 중은 1 줄고 완료는 1 는다")
        void 완료로_표시하면_완료다() {
            assertThat(표시한다(new Todo(1L, "가", null, null, false), true).todo().completed()).isTrue();
        }

        @Test
        @DisplayName("AC-06-2 완료된 할 일이 있을 때 진행 중으로 되돌리면 그 할 일은 진행 중이 되고 요약이 그에 맞게 바뀐다")
        void 되돌리면_진행_중이다() {
            assertThat(표시한다(new Todo(1L, "가", null, null, true), false).todo().completed()).isFalse();
        }

        @Test
        @DisplayName("AC-06-3 이미 완료된 할 일에 다시 완료로 표시하면 오류 없이 완료 상태가 유지된다")
        void 이미_완료인데_완료로_표시해도_완료가_유지된다() {
            assertThat(표시한다(new Todo(1L, "가", null, null, true), true).todo().completed()).isTrue();
        }

        @Test
        @DisplayName("AC-06-4 이미 진행 중인 할 일에 다시 진행 중으로 되돌리면 오류 없이 진행 중 상태가 유지된다")
        void 이미_진행_중인데_되돌려도_진행_중이_유지된다() {
            assertThat(표시한다(new Todo(1L, "가", null, null, false), false).todo().completed()).isFalse();
        }

        @Test
        @DisplayName("완료 표시로 바뀌는 것은 완료 여부뿐이다 (DATA_MODEL 4.3)")
        void 완료_여부_외의_값은_그대로다() {
            Todo todo = new Todo(7L, "가", "설명", LocalDate.of(2026, 11, 12), false);

            assertThat(표시한다(todo, true).todo()).isEqualTo(new Todo(7L, "가", "설명", LocalDate.of(2026, 11, 12), true));
        }

        @Test
        @DisplayName("AC-10-5 기한 지남이던 할 일이 있을 때 완료로 표시하면 기한 지남이 해제된다")
        void 완료로_표시하면_기한_지남이_해제된다() {
            Todo overdue = new Todo(1L, "가", null, LocalDate.of(2026, 11, 6), false);

            assertThat(표시한다(overdue, true).overdue()).isFalse();
        }

        @Test
        @DisplayName("AC-12-2 한 번도 만들어진 적 없는 할 일을 가리킬 때 완료 표시를 시도하면 같은 안내가 돌아오고, 다른 할 일은 바뀌지 않는다")
        void 없는_할_일에_완료_표시하면_할_일_없음_오류() {
            when(repository.update(eq(99L), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markCompleted(99L, true))
                    .isInstanceOfSatisfying(TodoException.class,
                            e -> assertThat(e.code()).isEqualTo(TodoErrorCode.TODO_NOT_FOUND));
        }
    }
}
